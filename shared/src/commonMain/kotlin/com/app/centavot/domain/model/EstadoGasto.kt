package com.app.centavot.domain.model

enum class EstadoGasto {
    /** Guardado en el teléfono, todavía no llega al backend. */
    PENDIENTE_SYNC,

    /** El backend ya hizo OCR/clasificación; falta que el usuario lo confirme. */
    PROCESADO,

    /** Revisado por el usuario. */
    CONFIRMADO,
}
