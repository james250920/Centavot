package com.app.centavot

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.app.centavot.core.util.ResultadoIngreso
import com.app.centavot.core.util.Sesion
import com.app.centavot.core.util.UsuarioSesion
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** La pantalla abierta: Credential Manager necesita una Activity para mostrar la hoja de cuentas. */
object ActividadActual {
    private var actual = WeakReference<Activity>(null)
    fun fijar(actividad: Activity) { actual = WeakReference(actividad) }
    fun obtener(): Activity? = actual.get()
}

/**
 * "Continuar con Google" con Firebase Authentication (proyecto centavot-67f60). Firebase solo sabe quién
 * entra; los movimientos siguen en Room, en el teléfono. La sesión queda guardada: sin señal, la app abre igual.
 */
class SesionFirebase(private val contexto: Context) : Sesion {
    private val auth = FirebaseAuth.getInstance()
    private val _usuario = MutableStateFlow(auth.currentUser?.aUsuario())

    override val disponible = true
    override val usuario: StateFlow<UsuarioSesion?> = _usuario.asStateFlow()

    init {
        auth.addAuthStateListener { _usuario.value = it.currentUser?.aUsuario() }
    }

    override suspend fun entrarConGoogle(): ResultadoIngreso {
        val actividad = ActividadActual.obtener() ?: return ResultadoIngreso.Error("Vuelve a abrir la app e inténtalo otra vez.")
        // El ID de cliente web lo genera el plugin de Google Services desde google-services.json.
        val opcion = GetSignInWithGoogleOption.Builder(contexto.getString(R.string.default_web_client_id)).build()
        val pedido = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        return try {
            val credencial = CredentialManager.create(actividad).getCredential(actividad, pedido).credential
            if (credencial !is CustomCredential || credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return ResultadoIngreso.Error("No se pudo entrar con Google. Inténtalo otra vez.")
            }
            val token = GoogleIdTokenCredential.createFrom(credencial.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            ResultadoIngreso.Listo
        } catch (e: GetCredentialCancellationException) {
            ResultadoIngreso.Cancelado
        } catch (e: NoCredentialException) {
            ResultadoIngreso.Error("Agrega una cuenta de Google en tu celular (Ajustes → Cuentas) y vuelve a intentar.")
        } catch (e: GetCredentialException) {
            ResultadoIngreso.Error("No se pudo entrar con Google. Inténtalo otra vez.")
        } catch (e: FirebaseNetworkException) {
            ResultadoIngreso.Error("Necesitas internet para entrar la primera vez.")
        } catch (e: Exception) {
            ResultadoIngreso.Error("No se pudo entrar con Google. Inténtalo otra vez.")
        }
    }

    override suspend fun cerrarSesion() {
        auth.signOut()
        // Que la próxima vez vuelva a mostrar la lista de cuentas.
        runCatching { CredentialManager.create(contexto).clearCredentialState(ClearCredentialStateRequest()) }
    }

    override suspend fun token(): String? =
        runCatching { auth.currentUser?.getIdToken(false)?.await()?.token }.getOrNull()

    private fun FirebaseUser.aUsuario() = UsuarioSesion(nombre = displayName, correo = email, uid = uid)
}
