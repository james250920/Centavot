package com.app.centavot.domain.model

/**
 * Para qué usa la persona Centavot. Cada modo muestra solo lo suyo: quien solo lleva su
 * presupuesto no ve ventas ni régimen, y la bodeguera no ve los gastos de su casa mezclados
 * con los del negocio. Cambiar de modo no borra nada: lo del otro modo vuelve al regresar.
 */
enum class Modo(val etiqueta: String, val categoria: Categoria) {
    NEGOCIO("Mi negocio", Categoria.NEGOCIO),
    PERSONAL("Mi plata personal", Categoria.PERSONAL),
}

/**
 * Las entradas de plata que se ven en [modo]. Lo que se saca de la caja para la casa es de los
 * dos: en el negocio resta de la caja y en lo personal es plata que entra.
 */
@kotlin.jvm.JvmName("ingresosDelModo")
fun List<Ingreso>.delModo(modo: Modo): List<Ingreso> = filter { it.categoria == modo.categoria || it.retiroDelNegocio }

/** Los gastos de [modo]. Un gasto antiguo sin clasificar se ve en los dos para poder clasificarlo. */
@kotlin.jvm.JvmName("gastosDelModo")
fun List<Gasto>.delModo(modo: Modo): List<Gasto> = filter { it.categoria == null || it.categoria == modo.categoria }

@kotlin.jvm.JvmName("cobrosDelModo")
fun List<Cobro>.delModo(modo: Modo): List<Cobro> = filter { it.categoria == modo.categoria }
