package com.example.adi.mausam.models.mainresponses

import com.example.adi.mausam.models.internalresponses.Coordinates
import com.example.adi.mausam.models.internalresponses.Main2
import java.io.Serializable

data class AirPollutionModel (
    val coord: Coordinates,
    val list: List<Main2>,
): Serializable