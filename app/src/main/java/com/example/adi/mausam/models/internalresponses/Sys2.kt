package com.example.adi.mausam.models.internalresponses

import java.io.Serializable

data class Sys2 (
    val timezone: Long,
    val id: Long,
    val name: String,
    val cod: Int
): Serializable
