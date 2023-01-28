package com.example.adi.mausam.models.mainresponses

import com.example.adi.mausam.models.internalresponses.City
import com.example.adi.mausam.models.internalresponses.EveryThreeHourForecast
import java.io.Serializable

data class WeatherForecastModel (
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<EveryThreeHourForecast>,
    val city: City
): Serializable