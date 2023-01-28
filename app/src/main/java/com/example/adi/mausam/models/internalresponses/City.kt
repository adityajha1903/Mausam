package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class City (
    val id: Long,
    val name: String,
    val coord: Coordinates,
    val country: String,
    val population: Long,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
): Serializable
