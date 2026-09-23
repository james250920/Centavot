package com.app.centavot

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform