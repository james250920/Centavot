package com.app.centavot.data.sync

import com.app.centavot.core.util.Sesion
import com.app.centavot.data.local.AbonoMetaEntity
import com.app.centavot.data.local.AjusteSyncEntity
import com.app.centavot.data.local.CambiosRemotos
import com.app.centavot.data.local.CobroEntity
import com.app.centavot.data.local.ContactoEntity
import com.app.centavot.data.local.EventoUsoDao
import com.app.centavot.data.local.GastoEntity
import com.app.centavot.data.local.IngresoEntity
import com.app.centavot.data.local.MetaEntity
import com.app.centavot.data.local.MontoPresupuestoEntity
import com.app.centavot.data.local.PartidaEntity
import com.app.centavot.data.local.PerfilDao
import com.app.centavot.data.local.PerfilEntity
import com.app.centavot.data.local.RegimenDao
import com.app.centavot.data.local.RegimenEntity
import com.app.centavot.data.local.SyncDao
import com.app.centavot.domain.model.EstadoGasto
import com.app.centavot.domain.model.OrigenGasto
import com.app.centavot.domain.model.TipoEventoUso
import com.app.centavot.domain.model.VERSION_AVISO_PRIVACIDAD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EstadoSync(val ocupado: Boolean = false, val ultimo: String? = null, val error: String? = null)

/**
 * Sincronización con el backend (/v1/sync), sin perder el modo sin conexión:
 *  1. Perfil y régimen: si cambiaron en el teléfono se suben; si no, se adopta lo del servidor.
 *  2. Sube lo anotado en sync_pendientes (lo llenan disparadores de SQLite) y quita solo lo que subió.
 *  3. Baja lo que cambió en el servidor desde la última vez (por ejemplo, lo anotado en la web).
 * Gana el último cambio que llega al servidor. Sin URL del backend o sin sesión, no hace nada.
 */
class Sincronizador(
    private val config: ConfigApi,
    private val api: ApiCentavot,
    private val sesion: Sesion,
    private val sync: SyncDao,
    private val perfilDao: PerfilDao,
    private val regimenDao: RegimenDao,
    private val eventos: EventoUsoDao,
    private val limpiador: LimpiadorLocal,
) {
    private val _estado = MutableStateFlow(EstadoSync())
    val estado: StateFlow<EstadoSync> = _estado.asStateFlow()
    private val candado = Mutex()

    /** Sincroniza una vez (si ya hay una en curso, espera a que termine y no repite). */
    suspend fun sincronizar() {
        if (!config.activa) return
        if (candado.isLocked) return
        candado.withLock {
            // Ley 29733: nada sale del teléfono hasta aceptar la versión vigente del aviso (que explica la sincronización).
            val aceptado = eventos.observarTodos().first()
                .any { it.tipo == TipoEventoUso.CONSENTIMIENTO_PRIVACIDAD.name && it.valor == VERSION_AVISO_PRIVACIDAD }
            if (!aceptado) return
            // Nunca subir datos de otra cuenta: los datos del teléfono deben ser de quien entró.
            val uid = sesion.usuario.value?.uid
            if (!uid.isNullOrEmpty() && sync.ajuste(AJ_DUENO) != uid) return
            val token = sesion.token() ?: return
            _estado.value = _estado.value.copy(ocupado = true)
            _estado.value = try {
                perfilYRegimen(token)
                subir(token)
                bajar(token)
                EstadoSync(ultimo = ahoraIso())
            } catch (e: ErrorApi) {
                // 403: el servidor no tiene el consentimiento de esta cuenta: se reenvía en la próxima sincronización.
                if (e.estado == 403) sync.guardarAjuste(AjusteSyncEntity(AJ_CONSENTIMIENTO, ""))
                EstadoSync(error = if (e.estado == 401) "La sesión venció: vuelve a entrar con Google." else e.message)
            } catch (e: Exception) {
                EstadoSync(error = "No se pudo conectar con el servidor.")
            }
        }
    }

    /** Ley 29733: "Borrar todos mis datos" borra también la cuenta en el servidor. true si se pudo (o no aplica). */
    suspend fun borrarCuentaEnServidor(): Boolean {
        if (!config.activa) return true
        val token = sesion.token() ?: return true
        return runCatching { api.borrarCuenta(token) }.isSuccess
    }

    /**
     * Los datos del teléfono son de una sola cuenta. Sin dueño (teléfonos de antes de este cambio) pasan a ser de esta
     * cuenta; si eran de otra, se borran y la app vuelve al aviso de privacidad para la nueva persona.
     */
    suspend fun asegurarDueno(uid: String) {
        if (uid.isEmpty()) return
        val dueno = sync.ajuste(AJ_DUENO)
        if (dueno == uid) return
        if (dueno != null) limpiador.limpiar()
        sync.guardarAjuste(AjusteSyncEntity(AJ_DUENO, uid))
    }

    /** Olvida lo pendiente y las marcas (después de borrar todos los datos del teléfono). */
    suspend fun reiniciar() {
        sync.vaciarPendientes()
        sync.borrarAjustes()
    }

    /** Sincroniza al entrar, unos segundos después de cada cambio y cada 5 minutos mientras la app vive. */
    @OptIn(FlowPreview::class)
    fun iniciar(alcance: CoroutineScope) {
        if (!config.activa) return
        alcance.launch {
            sesion.usuario.distinctUntilChanged { a, b -> a?.uid == b?.uid }.filter { it != null }.collect { u ->
                asegurarDueno(u!!.uid)
                sincronizar()
            }
        }
        alcance.launch {
            sync.observarPendientes().filter { it > 0 }.debounce(3_000).collect { sincronizar() }
        }
        alcance.launch {
            while (true) {
                delay(5 * 60_000L)
                sincronizar()
            }
        }
    }

    // ---------- 1. Perfil, régimen y consentimiento ----------

    private suspend fun perfilYRegimen(token: String) {
        val local = perfilDao.observar().first()
        val enviado = sync.ajuste(AJ_PERFIL)
        if (local != null && huella(local) != enviado) {
            api.guardarPerfil(
                token,
                PerfilDto(local.nombre, local.modo, local.rubro, local.celular, local.ingresoMensualCentimos, local.tasaAhorroDecimas, local.aceptaWhatsapp),
            )
            sync.guardarAjuste(AjusteSyncEntity(AJ_PERFIL, huella(local)))
        } else {
            api.obtenerPerfil(token)?.let { r ->
                val p = PerfilEntity(
                    nombre = r.nombre, rubro = r.rubro, ingresoMensualCentimos = r.ingresoMensualCentimos, tasaAhorroDecimas = r.tasaAhorroDecimas,
                    // El modo (negocio o personal) es de cada dispositivo: se conserva el de este teléfono.
                    modo = local?.modo ?: r.modo, celular = r.celular, aceptaWhatsapp = r.aceptaWhatsapp,
                )
                if (local == null || huella(p) != huella(local)) perfilDao.guardar(p)
                sync.guardarAjuste(AjusteSyncEntity(AJ_PERFIL, huella(p)))
            }
        }

        val regimen = regimenDao.observar().first()
        val codigo = regimen?.let { CODIGO_REGIMEN[it.nombre] }
        if (codigo != null && codigo != sync.ajuste(AJ_REGIMEN)) {
            api.elegirRegimen(token, codigo)
            sync.guardarAjuste(AjusteSyncEntity(AJ_REGIMEN, codigo))
        } else {
            api.obtenerRegimen(token)?.let { r ->
                val c = CODIGO_REGIMEN[r.nombre] ?: return@let
                if (regimen?.nombre != r.nombre) regimenDao.guardar(RegimenEntity(tipo = r.tipo, nombre = r.nombre, topeCentimos = r.topeCentimos, periodo = r.periodo))
                sync.guardarAjuste(AjusteSyncEntity(AJ_REGIMEN, c))
            }
        }

        val aceptado = eventos.observarTodos().first().any { it.tipo == TipoEventoUso.CONSENTIMIENTO_PRIVACIDAD.name && it.valor == VERSION_AVISO_PRIVACIDAD }
        if (aceptado && sync.ajuste(AJ_CONSENTIMIENTO) != VERSION_AVISO_PRIVACIDAD) {
            api.consentir(token, VERSION_AVISO_PRIVACIDAD)
            sync.guardarAjuste(AjusteSyncEntity(AJ_CONSENTIMIENTO, VERSION_AVISO_PRIVACIDAD))
        }
    }

    // ---------- 2. Subir ----------

    private suspend fun subir(token: String) {
        // La primera vez (o después de reiniciar) se sube todo lo que ya había en el teléfono.
        if (sync.ajuste(AJ_INICIAL) == null) {
            sync.encolarTodo()
            sync.guardarAjuste(AjusteSyncEntity(AJ_INICIAL, "1"))
        }
        val pendientes = sync.pendientes()
        if (pendientes.isEmpty()) return
        val hasta = pendientes.maxOf { it.marca }
        for (parte in pendientes.chunked(LOTE)) {
            val vivos = parte.filterNot { it.borrado }.groupBy({ it.tabla }, { it.id })
            fun ids(t: String) = vivos[t].orEmpty()
            val lote = LoteDto(
                partidas = if (ids("partidas").isEmpty()) emptyList() else sync.partidas(ids("partidas")).map { it.aDto() },
                montos = if (ids("montos_presupuesto").isEmpty()) emptyList() else sync.montos(ids("montos_presupuesto")).map { it.aDto() },
                contactos = if (ids("contactos").isEmpty()) emptyList() else sync.contactos(ids("contactos")).map { it.aDto() },
                ingresos = if (ids("ingresos").isEmpty()) emptyList() else sync.ingresos(ids("ingresos")).map { it.aDto() },
                gastos = if (ids("gastos").isEmpty()) emptyList() else sync.gastos(ids("gastos")).map { it.aDto() },
                cobros = if (ids("cobros").isEmpty()) emptyList() else sync.cobros(ids("cobros")).map { it.aDto() },
                metas = if (ids("metas").isEmpty()) emptyList() else sync.metas(ids("metas")).map { it.aDto() },
                abonos = if (ids("abonos_meta").isEmpty()) emptyList() else sync.abonos(ids("abonos_meta")).map { it.aDto() },
                // El servidor no borra montos sueltos: el monto de una partida borrada se va con ella.
                eliminados = parte.filter { it.borrado && it.tabla != "montos_presupuesto" }.map { EliminadoDto(ENTIDAD[it.tabla]!!, it.id) },
            )
            api.subir(token, lote)
        }
        sync.quitarHasta(hasta)
    }

    // ---------- 3. Bajar ----------

    private suspend fun bajar(token: String) {
        val bajada = api.bajar(token, sync.ajuste(AJ_CURSOR))
        val c = bajada.cambios
        // Los gastos anotados en el teléfono conservan su origen y proveedor.
        val previos = sync.gastos(c.gastos.map { it.id }).associateBy { it.id }
        val remotos = CambiosRemotos(
            partidas = c.partidas.filter { it.eliminadoEn == null }.map { it.aEntidad() },
            montos = c.montos.map { MontoPresupuestoEntity(it.partidaId, it.mes, it.montoCentimos) },
            contactos = c.contactos.filter { it.eliminadoEn == null }.map { ContactoEntity(it.id, it.nombre, it.telefono) },
            ingresos = c.ingresos.filter { it.eliminadoEn == null }.map { it.aEntidad() },
            gastos = c.gastos.filter { it.eliminadoEn == null }.map { it.aEntidad(previos[it.id]) },
            cobros = c.cobros.filter { it.eliminadoEn == null }.map { it.aEntidad() },
            metas = c.metas.filter { it.eliminadoEn == null }.map { MetaEntity(it.id, it.nombre, it.objetivoCentimos, it.categoria, it.creada, it.fechaLimite) },
            abonos = c.abonos.filter { it.eliminadoEn == null }.map { AbonoMetaEntity(it.id, it.metaId, it.montoCentimos, it.fecha) },
            borrados = mapOf(
                "partidas" to c.partidas.filter { it.eliminadoEn != null }.map { it.id },
                "contactos" to c.contactos.filter { it.eliminadoEn != null }.map { it.id },
                "ingresos" to c.ingresos.filter { it.eliminadoEn != null }.map { it.id },
                "gastos" to c.gastos.filter { it.eliminadoEn != null }.map { it.id },
                "cobros" to c.cobros.filter { it.eliminadoEn != null }.map { it.id },
                "metas" to c.metas.filter { it.eliminadoEn != null }.map { it.id },
                "abonos_meta" to c.abonos.filter { it.eliminadoEn != null }.map { it.id },
            ),
        )
        if (!remotos.vacio) sync.aplicarRemoto(remotos)
        sync.guardarAjuste(AjusteSyncEntity(AJ_CURSOR, bajada.servidorAhora))
    }

    private companion object {
        const val LOTE = 500
        const val AJ_CURSOR = "cursor"
        const val AJ_DUENO = "duenoDatos"
        const val AJ_INICIAL = "subidaInicial"
        const val AJ_PERFIL = "perfilEnviado"
        const val AJ_REGIMEN = "regimenEnviado"
        const val AJ_CONSENTIMIENTO = "consentimientoEnviado"
        val CODIGO_REGIMEN = mapOf("RUS · Categoría 1" to "RUS_1", "RUS · Categoría 2" to "RUS_2", "RER" to "RER")
        /** Tabla de Room → entidad del backend. */
        val ENTIDAD = mapOf(
            "partidas" to "partidas", "contactos" to "contactos", "ingresos" to "ingresos", "gastos" to "gastos",
            "cobros" to "cobros", "metas" to "metas", "abonos_meta" to "abonos",
        )
        /** Sin el modo: es una preferencia de vista de cada dispositivo, cambiarlo no sube el perfil. */
        fun huella(p: PerfilEntity) = listOf(p.nombre, p.rubro, p.ingresoMensualCentimos, p.tasaAhorroDecimas, p.celular, p.aceptaWhatsapp).joinToString("|")
    }
}

// ---------- Conversión Room ⇄ API ----------

private fun PartidaEntity.aDto() = PartidaDto(id, nombre, tipo, categoria, subcategoria, orden, tipoGasto, semanal, deuda)
private fun MontoPresupuestoEntity.aDto() = MontoDto(partidaId, mes, montoCentimos)
private fun ContactoEntity.aDto() = ContactoDto(id, nombre, telefono)
private fun IngresoEntity.aDto() = IngresoDto(id, montoCentimos, fecha, categoria, descripcion, retiroDelNegocio, partidaId)
private fun GastoEntity.aDto() = GastoDto(id, montoCentimos, fecha, categoria, subcategoria, descripcion, partidaId, corregidoManualmente)
private fun CobroEntity.aDto() = CobroDto(id, contactoId, motivo, montoCentimos, fecha, estado, fechaCobrado, tipo, adelantoCentimos, abonadoCentimos, categoria)
private fun MetaEntity.aDto() = MetaDto(id, nombre, objetivoCentimos, categoria, creada, fechaLimite)
private fun AbonoMetaEntity.aDto() = AbonoDto(id, metaId, montoCentimos, fecha)

private fun PartidaDto.aEntidad() = PartidaEntity(id, nombre, tipo, categoria, subcategoria, orden, tipoGasto, semanal, deuda)
private fun IngresoDto.aEntidad() = IngresoEntity(id, montoCentimos, fecha, categoria, descripcion, retiroDelNegocio, partidaId)
private fun CobroDto.aEntidad() = CobroEntity(id, contactoId, motivo, montoCentimos, fecha, estado, fechaCobrado, tipo, adelantoCentimos, abonadoCentimos, categoria)
private fun GastoDto.aEntidad(previo: GastoEntity?) = GastoEntity(
    id = id, montoCentimos = montoCentimos, fecha = fecha,
    origen = previo?.origen ?: OrigenGasto.TEXTO.name, estado = EstadoGasto.CONFIRMADO.name,
    categoria = categoria, subcategoria = subcategoria, proveedor = previo?.proveedor, descripcion = descripcion,
    corregidoManualmente = corregidoManualmente, partidaId = partidaId,
)

private fun ahoraIso(): String = kotlin.time.Clock.System.now().toString()
