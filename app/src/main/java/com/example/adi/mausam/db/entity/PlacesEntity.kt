package com.example.adi.mausam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placesTable")
data class PlacesEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var location: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var currentWeatherData: String = "",
    var aqi: String = "",
    var fiveDayWeatherData: String = ""
)