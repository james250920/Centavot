package com.app.centavot.domain.model

import kotlinx.datetime.LocalDate

enum class EstadoCobro { PENDIENTE, COBRADO }

/** Fiado o préstamo, o un pedido por entregar (el zapatero, la costurera, el de los menús). */
enum class TipoCobro(val etiqueta: String) {
    FIADO("Fiado o préstamo"),
    PEDIDO("Pedido"),
}

/** Dinero que un contacto le debe al usuario: un fiado, un préstamo o el saldo de un pedido. */
data class Cobro(
    val id: String,
    val contacto: Contacto,
    val motivo: String,
    val monto: Monto,
    val fecha: LocalDate,
    val estado: EstadoCobro = EstadoCobro.PENDIENTE,
    val fechaCobrado: LocalDate? = null,
    val tipo: TipoCobro = TipoCobro.FIADO,
    /** Lo que ya pagó por adelantado (en pedidos). */
    val adelanto: Monto = Monto.CERO,
    /** Suma de los abonos: pagos parciales hechos después de registrar el cobro. */
    val abonado: Monto = Monto.CERO,
) {
    val estaPendiente: Boolean get() = estado == EstadoCobro.PENDIENTE

    /** Lo que ya pagó: adelanto más abonos. */
    val pagado: Monto get() = adelanto + abonado

    /** Lo que falta cobrar. */
    val saldo: Monto get() = monto - pagado
}

data class DeudaContacto(val contacto: Contacto, val total: Monto, val cantidad: Int)

/** Lo que le deben al usuario, en total y por contacto (de mayor a menor). */
data class ResumenCobros(val totalPendiente: Monto, val porContacto: List<DeudaContacto>) {
    companion object {
        fun de(cobros: List<Cobro>): ResumenCobros {
            val pendientes = cobros.filter { it.estaPendiente }
            return ResumenCobros(
                totalPendiente = pendientes.map { it.saldo }.sumar(),
                porContacto = pendientes.groupBy { it.contacto.id }
                    .map { (_, delContacto) ->
                        DeudaContacto(delContacto.first().contacto, delContacto.map { it.saldo }.sumar(), delContacto.size)
                    }
                    .sortedByDescending { it.total },
            )
        }
    }
}

/** Mensaje amable para recordar un cobro por WhatsApp. */
fun Cobro.mensajeRecordatorio(nombreNegocio: String?): String {
    val firma = nombreNegocio?.takeIf { it.isNotBlank() }?.let { " Saludos, $it." }.orEmpty()
    val que = when (tipo) {
        TipoCobro.FIADO -> "tienes pendiente ${saldo.enSoles()} por \"$motivo\""
        TipoCobro.PEDIDO -> "tu pedido \"$motivo\" tiene un saldo de ${saldo.enSoles()}"
    }
    return "Hola ${contacto.nombre}, te escribo para recordarte que $que.$firma"
}
