package com.app.centavot.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CelularTest {
    @Test
    fun quedaEnElFormatoDeWaMe() {
        assertEquals("+51987654321", normalizarCelular("987 654 321"))
        assertEquals("+51987654321", normalizarCelular("987-654-321"))
        assertEquals("+51987654321", normalizarCelular("51987654321"))
        assertEquals("+51987654321", normalizarCelular("+51 987 654 321"))
    }

    @Test
    fun vacioEsNull() {
        assertNull(normalizarCelular(""))
        assertNull(normalizarCelular(null))
    }

    @Test
    fun rechazaLoQueNoEsUnCelular() {
        assertFailsWith<IllegalArgumentException> { normalizarCelular("12345") }
        assertFailsWith<IllegalArgumentException> { normalizarCelular("887654321") }
    }
}
