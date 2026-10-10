package com.app.centavot.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class GraficoTest {
    @Test
    fun ejeEnSolesRedondos() {
        assertEquals(listOf("S/ 7,200", "S/ 3,600", "S/ 0"), etiquetasEje(720_000))
    }

    @Test
    fun ejeRedondeaAlSolMasCercano() {
        // Máximo S/ 4,300.70 → mitad S/ 2,150.35.
        assertEquals(listOf("S/ 4,301", "S/ 2,150", "S/ 0"), etiquetasEje(430_070))
    }
}
