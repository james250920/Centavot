package com.app.centavot.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Qué partidas del presupuesto se ofrecen al registrar un gasto, un ingreso o una venta (paso 38). */
class PartidasAlRegistrarTest {

    private fun gasto(nombre: String, tipo: TipoGasto, orden: Int, categoria: Categoria = Categoria.PERSONAL) =
        Partida(nombre, nombre, TipoPartida.GASTO, categoria, orden = orden, tipoGasto = tipo)

    private fun ingreso(nombre: String, orden: Int, categoria: Categoria = Categoria.PERSONAL) =
        Partida(nombre, nombre, TipoPartida.INGRESO, categoria, orden = orden)

    private val personal = listOf(
        ingreso("Sueldo", 0),
        gasto("Ahorro del mes", TipoGasto.AHORRO, 1),
        gasto("Salidas", TipoGasto.PRESCINDIBLE, 2),
        gasto("Alquiler", TipoGasto.FIJO, 3),
        gasto("Comida", TipoGasto.VARIABLE, 4),
        gasto("Luz", TipoGasto.FIJO, 5),
        ingreso("Comisiones", 6),
        // Agregada por el usuario con "Agregar" en prescindibles: va al final de su grupo.
        gasto("Regalos y otros", TipoGasto.PRESCINDIBLE, 7),
    )

    @Test
    fun alRegistrarUnGastoSalenSoloLasDeGastoEnElOrdenDeLaGuia() {
        val nombres = personal.paraRegistrarGasto().map { it.nombre }
        assertEquals(listOf("Alquiler", "Luz", "Comida", "Salidas", "Regalos y otros"), nombres)
    }

    @Test
    fun elAhorroNoSeOfreceComoGastoPorqueSeSeparaDesdeMetas() {
        assertTrue(personal.paraRegistrarGasto().none { it.clase == TipoGasto.AHORRO })
    }

    @Test
    fun unaPartidaSinTipoElegidoTomaElDeSuSubcategoria() {
        val vieja = Partida("m", "Mercadería", TipoPartida.GASTO, Categoria.NEGOCIO, SubcategoriaGasto.MERCADERIA, orden = 0)
        val alquiler = Partida("a", "Alquiler del local", TipoPartida.GASTO, Categoria.NEGOCIO, SubcategoriaGasto.ALQUILER, orden = 1)
        // Mercadería es variable y alquiler es fijo: el fijo va primero aunque se haya creado después.
        assertEquals(listOf("Alquiler del local", "Mercadería"), listOf(vieja, alquiler).paraRegistrarGasto().map { it.nombre })
    }

    @Test
    fun alRegistrarUnIngresoSalenLasFuentesEnSuOrden() {
        assertEquals(listOf("Sueldo", "Comisiones"), personal.fuentesDeIngreso().map { it.nombre })
    }

    @Test
    fun unaVentaNoPreguntaLaFuenteSiElNegocioTieneUnaSola() {
        val negocio = listOf(ingreso("Ventas", 0, Categoria.NEGOCIO), gasto("Alquiler", TipoGasto.FIJO, 1, Categoria.NEGOCIO))
        assertTrue(negocio.fuentesParaVenta().isEmpty())
    }

    @Test
    fun unaVentaPreguntaLaFuenteSiElNegocioTieneVarias() {
        val negocio = listOf(
            ingreso("Ventas", 0, Categoria.NEGOCIO),
            gasto("Alquiler", TipoGasto.FIJO, 1, Categoria.NEGOCIO),
            ingreso("Servicios", 2, Categoria.NEGOCIO),
        )
        assertEquals(listOf("Ventas", "Servicios"), negocio.fuentesParaVenta().map { it.nombre })
    }
}
