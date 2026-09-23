package com.app.centavot.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProximidadTopeTest {

    private val tope = Monto.soles(8_000)

    @Test
    fun sinGastosNoHayAlerta() {
        val proximidad = ProximidadTope(acumulado = Monto.CERO, tope = tope)

        assertEquals(0, proximidad.porcentaje)
        assertEquals(NivelAlerta.NINGUNA, proximidad.nivelAlerta)
        assertEquals(tope, proximidad.restante)
    }

    @Test
    fun justoDebajoDel80NoAlerta() {
        val proximidad = ProximidadTope(acumulado = Monto(639_999), tope = tope)

        assertEquals(79, proximidad.porcentaje)
        assertEquals(NivelAlerta.NINGUNA, proximidad.nivelAlerta)
    }

    @Test
    fun cruzarCadaUmbralCambiaLaAlerta() {
        assertEquals(NivelAlerta.AVISO_80, ProximidadTope(Monto.soles(6_400), tope).nivelAlerta)
        assertEquals(NivelAlerta.AVISO_90, ProximidadTope(Monto.soles(7_200), tope).nivelAlerta)
        assertEquals(NivelAlerta.TOPE_ALCANZADO, ProximidadTope(Monto.soles(8_000), tope).nivelAlerta)
    }

    @Test
    fun pasarseDelTopeNoDaRestanteNegativo() {
        val proximidad = ProximidadTope(acumulado = Monto.soles(9_000), tope = tope)

        assertEquals(112, proximidad.porcentaje)
        assertEquals(NivelAlerta.TOPE_ALCANZADO, proximidad.nivelAlerta)
        assertEquals(Monto.CERO, proximidad.restante)
    }

    @Test
    fun topeEnCeroEsInvalido() {
        assertFailsWith<IllegalArgumentException> { ProximidadTope(Monto.CERO, Monto.CERO) }
    }
}
