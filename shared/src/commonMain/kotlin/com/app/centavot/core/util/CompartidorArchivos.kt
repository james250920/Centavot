package com.app.centavot.core.util

/** Abre el menú de compartir del sistema con un archivo de texto. Lo implementa cada plataforma. */
fun interface CompartidorArchivos {
    fun compartir(nombreArchivo: String, contenido: String, tipoMime: String)

    /** Igual, para un archivo binario como un PDF. */
    fun compartir(nombreArchivo: String, contenido: ByteArray, tipoMime: String) {}

    /** Borra los archivos que se generaron para compartir (pueden tener datos del usuario). */
    fun borrarArchivos() {}
}
