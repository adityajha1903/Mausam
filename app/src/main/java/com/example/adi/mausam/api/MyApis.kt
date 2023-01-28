package com.example.adi.mausam.api

import com.example.adi.mausam.models.mainresponses.AirPollutionModel
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel
import com.example.adi.mausam.models.mainresponses.WeatherForecastModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MyApis {

    @GET("2.5/forecast")
    fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String?
    ): Call<WeatherForecastModel>

    @GET("2.5/air_pollution")
    fun getAirPollution(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String?
    ): Call<AirPollutionModel>

    @GET("2.5/weather")
    fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String?
    ): Call<CurrentWeatherModel>
}