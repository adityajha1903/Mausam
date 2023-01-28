package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class Main3 (
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
): Serializable
