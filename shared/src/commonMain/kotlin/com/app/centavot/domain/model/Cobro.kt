package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate

enum class EstadoCobro { PENDIENTE, COBRADO }

/** Dinero que un contacto le debe al usuario (fiado o préstamo). */
data class Cobro(
    val id: String,
    val contacto: Contacto,
    val motivo: String,
    val monto: Monto,
    val fecha: LocalDate,
    val estado: EstadoCobro = EstadoCobro.PENDIENTE,
    val fechaCobrado: LocalDate? = null,
) {
    val estaPendiente: Boolean get() = estado == EstadoCobro.PENDIENTE
}

data class DeudaContacto(val contacto: Contacto, val total: Monto, val cantidad: Int)

/** Lo que le deben al usuario, en total y por contacto (de mayor a menor). */
data class ResumenCobros(val totalPendiente: Monto, val porContacto: List<DeudaContacto>) {
    companion object {
        fun de(cobros: List<Cobro>): ResumenCobros {
            val pendientes = cobros.filter { it.estaPendiente }
            return ResumenCobros(
                totalPendiente = pendientes.map { it.monto }.sumar(),
                porContacto = pendientes.groupBy { it.contacto.id }
                    .map { (_, delContacto) ->
                        DeudaContacto(delContacto.first().contacto, delContacto.map { it.monto }.sumar(), delContacto.size)
                    }
                    .sortedByDescending { it.total },
            )
        }
    }
}
