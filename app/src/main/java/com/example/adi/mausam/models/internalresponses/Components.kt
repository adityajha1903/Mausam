package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class Components (
    val co: Double,
    val no: Double,
    val no2: Double,
    val o3: Double,
    val so2: Double,
    val pm2_5: Double,
    val pm10: Double,
    val nh3: Double
): Serializable
