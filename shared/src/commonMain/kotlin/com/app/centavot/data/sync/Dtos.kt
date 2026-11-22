package com.app.centavot.data.sync

import kotlinx.serialization.Serializable

// Lo que viaja por /v1 (los mismos nombres que el backend y la web).

@Serializable
data class PerfilDto(
    val nombre: String,
    val modo: String,
    val rubro: String? = null,
    val celular: String? = null,
    val ingresoMensualCentimos: Long = 0,
    val tasaAhorroDecimas: Int = 0,
    val aceptaWhatsapp: Boolean = false,
)

@Serializable data class RegimenDto(val tipo: String, val nombre: String, val topeCentimos: Long, val periodo: String)
@Serializable data class ElegirRegimenDto(val codigo: String)
@Serializable data class ConsentimientoDto(val version: String, val plataforma: String)

@Serializable
data class PartidaDto(
    val id: String, val nombre: String, val tipo: String, val categoria: String, val subcategoria: String? = null,
    val orden: Int = 0, val tipoGasto: String? = null, val semanal: Boolean = false, val deuda: Boolean = false,
    val eliminadoEn: String? = null,
)

@Serializable data class MontoDto(val partidaId: String, val mes: String, val montoCentimos: Long)

@Serializable data class ContactoDto(val id: String, val nombre: String, val telefono: String? = null, val eliminadoEn: String? = null)

@Serializable
data class IngresoDto(
    val id: String, val montoCentimos: Long, val fecha: String, val categoria: String = "NEGOCIO", val descripcion: String? = null,
    val retiroDelNegocio: Boolean = false, val partidaId: String? = null, val eliminadoEn: String? = null,
)

@Serializable
data class GastoDto(
    val id: String, val montoCentimos: Long, val fecha: String, val categoria: String? = null, val subcategoria: String? = null,
    val descripcion: String? = null, val partidaId: String? = null, val corregidoManualmente: Boolean = true, val eliminadoEn: String? = null,
)

@Serializable
data class CobroDto(
    val id: String, val contactoId: String, val motivo: String, val montoCentimos: Long, val fecha: String,
    val estado: String = "PENDIENTE", val fechaCobrado: String? = null, val tipo: String = "FIADO",
    val adelantoCentimos: Long = 0, val abonadoCentimos: Long = 0, val categoria: String = "NEGOCIO", val eliminadoEn: String? = null,
)

@Serializable
data class MetaDto(
    val id: String, val nombre: String, val objetivoCentimos: Long, val categoria: String, val creada: String,
    val fechaLimite: String? = null, val eliminadoEn: String? = null,
)

@Serializable data class AbonoDto(val id: String, val metaId: String, val montoCentimos: Long, val fecha: String, val eliminadoEn: String? = null)

@Serializable data class EliminadoDto(val entidad: String, val id: String)

@Serializable
data class LoteDto(
    val partidas: List<PartidaDto> = emptyList(),
    val montos: List<MontoDto> = emptyList(),
    val contactos: List<ContactoDto> = emptyList(),
    val ingresos: List<IngresoDto> = emptyList(),
    val gastos: List<GastoDto> = emptyList(),
    val cobros: List<CobroDto> = emptyList(),
    val metas: List<MetaDto> = emptyList(),
    val abonos: List<AbonoDto> = emptyList(),
    val eliminados: List<EliminadoDto> = emptyList(),
)

@Serializable data class BajadaDto(val servidorAhora: String, val cambios: LoteDto)

// OCR de boletas: POST /v1/comprobantes/leer
@Serializable data class LeerComprobanteDto(val imagenBase64: String, val tipo: String)
@Serializable data class GastoBorradorDto(val montoCentimos: Long, val fecha: String, val subcategoria: String? = null, val descripcion: String? = null)
@Serializable data class BorradorDto(val gasto: GastoBorradorDto? = null, val revisar: List<String> = emptyList())
