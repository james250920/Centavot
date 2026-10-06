package com.app.centavot.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object RutaInicio

@Serializable
data object RutaMovimientos

@Serializable
data object RutaReporte

@Serializable
data object RutaRegimen

/** [id] null = registrar un gasto nuevo. */
@Serializable
data class RutaGasto(val id: String? = null)

@Serializable
data object RutaCobros

/** [id] null = registrar un cobro nuevo. */
@Serializable
data class RutaCobro(val id: String? = null)

@Serializable
data object RutaContactos

@Serializable
data object RutaAjustes

@Serializable
data object RutaActividad

@Serializable
data object RutaNotificaciones

/** [id] null = registrar ventas nuevas. */
@Serializable
data class RutaVenta(val id: String? = null)

@Serializable
data object RutaAyuda
