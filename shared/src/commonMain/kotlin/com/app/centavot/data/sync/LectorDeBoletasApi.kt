package com.app.centavot.data.sync

import com.app.centavot.core.util.Sesion
import com.app.centavot.domain.model.Monto
import com.app.centavot.domain.model.SubcategoriaGasto
import com.app.centavot.domain.repository.LectorDeBoletas
import com.app.centavot.domain.repository.LecturaBoleta
import kotlinx.datetime.LocalDate
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Manda la foto al backend (POST /v1/comprobantes/leer) y convierte el borrador en lo que entiende la pantalla. */
class LectorDeBoletasApi(
    private val config: ConfigApi,
    private val api: ApiCentavot,
    private val sesion: Sesion,
) : LectorDeBoletas {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun leer(jpeg: ByteArray): LecturaBoleta {
        if (!config.activa) return LecturaBoleta.Error("La lectura de boletas no está disponible en esta versión.")
        val token = sesion.token() ?: return LecturaBoleta.Error("Entra con Google para leer boletas.")
        return try {
            val b = api.leerComprobante(token, Base64.encode(jpeg))
            val g = b.gasto
            LecturaBoleta.Leida(
                monto = g?.let { Monto(it.montoCentimos) },
                fecha = g?.fecha?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                subcategoria = g?.subcategoria?.let { s -> SubcategoriaGasto.entries.firstOrNull { it.name == s } },
                descripcion = g?.descripcion,
                revisar = b.revisar,
            )
        } catch (e: ErrorApi) {
            // 422 foto inválida, 429 tope del día, 503 no disponible: el servidor manda el mensaje para la persona.
            LecturaBoleta.Error(if (e.estado == 401) "La sesión venció: vuelve a entrar con Google." else e.message ?: "No se pudo leer la boleta.")
        } catch (e: Exception) {
            LecturaBoleta.Error("Sin conexión con el servidor. Revisa tu internet o anota el gasto a mano.")
        }
    }
}
