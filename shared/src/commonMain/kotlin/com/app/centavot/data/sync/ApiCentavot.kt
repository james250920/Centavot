package com.app.centavot.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Dirección del backend. Vacía = sin sincronización (la app funciona solo en el teléfono, como antes). */
data class ConfigApi(val url: String) {
    val activa get() = url.isNotBlank()
}

class ErrorApi(val estado: Int, mensaje: String) : Exception(mensaje)

/** Cliente HTTP del backend de Centavot (/v1). Montos en céntimos, fechas AAAA-MM-DD. */
class ApiCentavot(private val config: ConfigApi, private val http: HttpClient) {
    internal val json = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }

    private suspend fun <T> pedir(
        token: String, metodo: HttpMethod, ruta: String, cuerpo: String?, lector: KSerializer<T>?, esperaMs: Long? = null,
    ): T? {
        val r = http.request("${config.url.trimEnd('/')}/v1$ruta") {
            method = metodo
            if (esperaMs != null) timeout { requestTimeoutMillis = esperaMs; socketTimeoutMillis = esperaMs }
            header("Authorization", "Bearer $token")
            if (cuerpo != null) setBody(TextContent(cuerpo, ContentType.Application.Json))
        }
        if (r.status == HttpStatusCode.NoContent || r.status == HttpStatusCode.NotFound) return null
        val texto = r.bodyAsText()
        if (r.status.value !in 200..299) {
            val mensaje = runCatching { json.decodeFromString(RespuestaError.serializer(), texto).mensaje }.getOrNull()
            throw ErrorApi(r.status.value, mensaje ?: "Error ${r.status.value}")
        }
        return lector?.let { json.decodeFromString(it, texto) }
    }

    suspend fun obtenerPerfil(token: String) = pedir(token, HttpMethod.Get, "/perfil", null, PerfilDto.serializer())
    suspend fun guardarPerfil(token: String, p: PerfilDto) {
        pedir<Unit>(token, HttpMethod.Put, "/perfil", json.encodeToString(PerfilDto.serializer(), p), null)
    }
    suspend fun obtenerRegimen(token: String) = pedir(token, HttpMethod.Get, "/regimen", null, RegimenDto.serializer())
    suspend fun elegirRegimen(token: String, codigo: String) {
        pedir<Unit>(token, HttpMethod.Put, "/regimen", json.encodeToString(ElegirRegimenDto.serializer(), ElegirRegimenDto(codigo)), null)
    }
    suspend fun consentir(token: String, version: String) {
        pedir<Unit>(token, HttpMethod.Post, "/consentimientos", json.encodeToString(ConsentimientoDto.serializer(), ConsentimientoDto(version, "ANDROID")), null)
    }
    suspend fun subir(token: String, lote: LoteDto) {
        pedir<Unit>(token, HttpMethod.Post, "/sync", json.encodeToString(LoteDto.serializer(), lote), null)
    }
    suspend fun bajar(token: String, desde: String?): BajadaDto =
        pedir(token, HttpMethod.Get, if (desde == null) "/sync" else "/sync?desde=$desde", null, BajadaDto.serializer())
            ?: throw ErrorApi(404, "El servidor no respondió la sincronización.")
    /** OCR: la lectura puede tardar hasta ~85 s en el servidor, por eso espera más que el resto. */
    suspend fun leerComprobante(token: String, jpegBase64: String): BorradorDto =
        pedir(token, HttpMethod.Post, "/comprobantes/leer", json.encodeToString(LeerComprobanteDto.serializer(), LeerComprobanteDto(jpegBase64, "image/jpeg")), BorradorDto.serializer(), esperaMs = 100_000)
            ?: throw ErrorApi(404, "El servidor no tiene la lectura de boletas.")

    suspend fun borrarCuenta(token: String) {
        pedir<Unit>(token, HttpMethod.Delete, "/cuenta", null, null)
    }
}

@Serializable internal data class RespuestaError(val error: String? = null, val mensaje: String? = null)
