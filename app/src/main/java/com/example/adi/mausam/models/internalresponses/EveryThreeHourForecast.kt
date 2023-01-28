package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class EveryThreeHourForecast (
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double,
    val rain: Rain,
    val sys: Sys,
    val dt_text: String
): Serializable
