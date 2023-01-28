package com.example.adi.mausam.models.mainresponses

import com.example.adi.mausam.models.internalresponses.*
import java.io.Serializable

data class CurrentWeatherModel (
    val coord: Coordinates,
    val weather: List<Weather>,
    val base: String,
    val main: Main3,
    val visibility: Int,
    val wind: Wind2,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys2
): Serializable