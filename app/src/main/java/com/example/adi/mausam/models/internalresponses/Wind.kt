package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class Wind (
    val speed: Float,
    val deg: Int,
    val gust: Float
): Serializable
