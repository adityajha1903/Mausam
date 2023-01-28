package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class Main2 (
    val dt: Long,
    val main: MainAqi,
    val components: Components,
): Serializable
