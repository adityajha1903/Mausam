package com.example.adi.mausam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.adi.mausam.db.entity.PlacesEntity
import com.example.adi.mausam.models.internalresponses.EveryThreeHourForecast
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object Objects {

    const val API_ID = "e0f22ac7a52b114a33d9b7cec301365f"
    const val BASE_URL_CURRENT_WEATHER = "https://api.openweathermap.org/data/"
    const val BASE_URL_WEATHER_FORECAST = "https://api.openweathermap.org/data/"
    const val BASE_URL_AIR_POLLUTION = "http://api.openweathermap.org/data/"

    const val LOCATION_REQ_CODE = 1001
    const val PLACE_ID = "place_id"

    @Suppress("DEPRECATION")
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        val result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        return result
    }

    fun kelvinToCelcius(temp: Double): Int {
        return (temp - 273.15).toInt()
    }

    fun getRvHolderColor(context: Context, main: String, icon: String): Int {
        return when(main) {
            "Thunderstorm" -> {context.resources.getColor(R.color.thunderstorm, null)}
            "Drizzle" -> {context.resources.getColor(R.color.thunderstorm, null)}
            "Rain" -> {context.resources.getColor(R.color.rain, null)}
            "Snow" -> {context.resources.getColor(R.color.snow, null)}
            "Clear" -> {
                when(icon) {
                    "01d" -> { context.resources.getColor(R.color.clear_day, null) }
                    "01n" -> { context.resources.getColor(R.color.clear_night, null) }
                    else -> {context.resources.getColor(R.color.clear_day, null)}
                }
            }
            "Clouds" -> {context.resources.getColor(R.color.clouds, null)}
            else -> {context.resources.getColor(R.color.mist, null)}
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun setTempIcon(context: Context, icon: String, id: Int): Drawable? {

        return when (id) {
            200, 201, 202, 210, 211, 212, 221, 230, 231, 232 -> context.resources.getDrawable(R.drawable.thunderstorm_icon, null)
            300, 301, 302, 310, 311, 312, 313, 314, 321 -> context.resources.getDrawable(R.drawable.drizzle_icon, null)
            500, 501, 502, 503, 504 -> context.resources.getDrawable(R.drawable.rain_day_icon, null)
            511, 600, 601, 602, 611, 612, 613, 615, 616, 620, 621, 622 -> context.resources.getDrawable(R.drawable.snow_icon, null)
            520, 521, 522, 531 -> context.resources.getDrawable(R.drawable.rain_night_icon, null)
            800 -> {
                return when(icon) {
                    "01d" -> context.resources.getDrawable(R.drawable.clear_day_icon, null)
                    else -> context.resources.getDrawable(R.drawable.clear_night_icon, null)
                }
            }
            801 -> {
                return when(icon){
                    "02d" -> context.resources.getDrawable(R.drawable.few_clouds_day_icon, null)
                    else -> context.resources.getDrawable(R.drawable.few_clouds_night_icon, null)
                }
            }
            802 -> context.resources.getDrawable(R.drawable.scattered_clouds_icon, null)
            803, 804 -> context.resources.getDrawable(R.drawable.broken_clouds_icon, null)
            else -> context.resources.getDrawable(R.drawable.mist_icon, null)
        }
    }

    fun setTextColorAsAqi(context: Context, aqi: Int): Int {
        return when (aqi) {
            1 -> context.resources.getColor(R.color.green, null)
            2 -> context.resources.getColor(R.color.yellow, null)
            3 -> context.resources.getColor(R.color.orange, null)
            4 -> context.resources.getColor(R.color.red, null)
            else -> context.resources.getColor(R.color.purple, null)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getAqiIcon(context: Context, aqi: Int): Drawable? {
        return when (aqi) {
            1 -> context.resources.getDrawable(R.drawable.leaf, null)
            2 -> context.resources.getDrawable(R.drawable.yellow_leaf, null)
            3 -> context.resources.getDrawable(R.drawable.mask, null)
            4 -> context.resources.getDrawable(R.drawable.gas_mask, null)
            else -> context.resources.getDrawable(R.drawable.skull, null)
        }
    }

    fun msIntoKh(ms: Float): Float {
        val kh: Float = ms * 3.6f
        return (kh * 100.0f).roundToInt() / 100.0f
    }

    fun roundOff(num: Double) : Double {
        return (num * 100.0).roundToInt() / 100.0
    }

    @SuppressLint("SimpleDateFormat")
    fun formatTime(unix: Long): String {
        val unixIst = unix + 19800
        val date = Date(unixIst * 1000)
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("IST")
        return simpleDateFormat.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun getDayName(unix: Long): String {
        val unixIst = unix + 19800
        val date = Date(unixIst * 1000)
        val simpleDateFormat = SimpleDateFormat("E")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("IST")
        return simpleDateFormat.format(date)
    }

    fun getMinMaxTemp(date: Int, list: List<EveryThreeHourForecast>, currentWeatherModel: CurrentWeatherModel): Array<Double> {
        val ret = arrayOf(Double.MAX_VALUE, Double.MIN_VALUE)

        var start = 0
        var end = 0
        var count = 1
        for ((index, data) in list.withIndex()) {
            if (index == 0 && formatTime(data.dt).subSequence(11, 16).toString() == "23:30" && date == 1) {
                ret[0] = currentWeatherModel.main.temp_min
                ret[1] = currentWeatherModel.main.temp_max
                return ret
            }
            if (formatTime(data.dt).subSequence(11, 16).toString() == "23:30" || index == list.size - 1) {
                if (count == date) {
                    end = index
                    break
                } else {
                    start = index
                    count++
                }
            }
        }

        while (start < end) {
            if (list[start].main.temp_min < ret[0]) {
                ret[0] = list[start].main.temp_min
            }
            if (list[start].main.temp_max > ret[1]) {
                ret[1] = list[start].main.temp_max
            }
            start++
        }

        return ret
    }

    fun getMiddleIndexOfEveryDay(day: Int, list: List<EveryThreeHourForecast>): Int {
        var start = 0
        var end = 0
        var count = 1
        for ((index, data) in list.withIndex()) {
            if (formatTime(data.dt).subSequence(11, 16).toString() == "23:30" || index == list.size - 1) {
                if (count == day) {
                    end = index
                    break
                } else {
                    start = index
                    count++
                }
            }
        }
        return (start + end)/2
    }

    fun getAllAvailablePlaces(): ArrayList<PlacesEntity> {
        val list = ArrayList<PlacesEntity>()

        list.add(PlacesEntity(location = "Andaman And Nicobar", latitude = 11.66, longitude = 92.73))
        list.add(PlacesEntity(location = "Andhra Pradesh", latitude = 14.75, longitude = 78.57))
        list.add(PlacesEntity(location = "Arunachal Pradesh", latitude = 27.10, longitude = 93.61))
        list.add(PlacesEntity(location = "Assam", latitude = 26.74, longitude = 94.21))
        list.add(PlacesEntity(location = "Bihar", latitude = 25.78, longitude = 87.47))
        list.add(PlacesEntity(location = "Chandigarh", latitude = 30.71, longitude = 76.78))
        list.add(PlacesEntity(location = "Chhattisgarh", latitude = 22.09, longitude = 82.15))
        list.add(PlacesEntity(location = "Dadra And Nagar Haveli", latitude = 20.26, longitude = 73.01))
        list.add(PlacesEntity(location = "Delhi", latitude = 28.67, longitude = 77.23))
        list.add(PlacesEntity(location = "Goa", latitude = 15.49, longitude = 73.82))
        list.add(PlacesEntity(location = "Haryana", latitude = 28.45, longitude = 77.02))
        list.add(PlacesEntity(location = "Himachal Pradesh", latitude = 31.10, longitude = 77.16))
        list.add(PlacesEntity(location = "Jammu And Kashmir", latitude = 34.29, longitude = 74.46))
        list.add(PlacesEntity(location = "Jharkhand", latitude = 23.80, longitude = 86.41))
        list.add(PlacesEntity(location = "Karnataka", latitude = 12.57, longitude = 76.92))
        list.add(PlacesEntity(location = "Kerala", latitude = 8.90, longitude = 76.56))
        list.add(PlacesEntity(location = "Lakshadweep", latitude = 10.56, longitude = 72.63))
        list.add(PlacesEntity(location = "Madhya Pradesh", latitude = 21.30, longitude = 76.13))
        list.add(PlacesEntity(location = "Maharashtra", latitude = 19.25, longitude = 73.16))
        list.add(PlacesEntity(location = "Manipur", latitude = 24.80, longitude = 93.95))
        list.add(PlacesEntity(location = "Meghalaya", latitude = 25.57, longitude = 91.88))
        list.add(PlacesEntity(location = "Mizoram", latitude = 23.71, longitude = 92.72))
        list.add(PlacesEntity(location = "Nagaland", latitude = 25.66, longitude = 94.11))
        list.add(PlacesEntity(location = "Orissa", latitude = 19.82, longitude = 85.90))
        list.add(PlacesEntity(location = "Puducherry", latitude = 11.93, longitude = 79.83))
        list.add(PlacesEntity(location = "Punjab", latitude = 31.52, longitude = 75.98))
        list.add(PlacesEntity(location = "Rajasthan", latitude = 26.45, longitude = 74.64))
        list.add(PlacesEntity(location = "Sikkim", latitude = 27.33, longitude = 88.61))
        list.add(PlacesEntity(location = "Tamil Nadu", latitude = 12.92, longitude = 79.15))
        list.add(PlacesEntity(location = "Tripura", latitude = 23.83, longitude = 91.28))
        list.add(PlacesEntity(location = "Uttar Pradesh", latitude = 27.60, longitude = 78.05))
        list.add(PlacesEntity(location = "Uttaranchal", latitude = 30.32, longitude = 78.05))
        list.add(PlacesEntity(location = "West Bengal", latitude = 22.58, longitude = 88.33))

        return list
    }
}