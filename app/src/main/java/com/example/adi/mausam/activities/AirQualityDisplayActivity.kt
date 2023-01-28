package com.example.adi.mausam.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.adi.mausam.Objects
import com.example.adi.mausam.R
import com.example.adi.mausam.databinding.ActivityAirQualityDisplayBinding
import com.example.adi.mausam.db.database.PlacesDatabase
import com.example.adi.mausam.models.mainresponses.AirPollutionModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class AirQualityDisplayActivity : AppCompatActivity() {

    private var binding: ActivityAirQualityDisplayBinding? = null

    private var placeAirPollutionModel: AirPollutionModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_air_quality_display)

        binding = ActivityAirQualityDisplayBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.airQualityToolBar)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
        }
        binding?.backPressIB2?.setOnClickListener {
            finish()
        }

        binding?.moreOnAqiLL?.setOnClickListener {
            val url = "https://en.wikipedia.org/wiki/Air_quality_index"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        fetchDataFromDatabase()
    }

    private fun fetchDataFromDatabase() {
        val placeId = intent.getIntExtra(Objects.PLACE_ID, 0)
        val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
        val placesDao = placesDb.placesDao()
        val gson = Gson()
        lifecycleScope.launch {
            val place = placesDao.getPlaceById(placeId)
            placeAirPollutionModel = gson.fromJson(place.aqi, AirPollutionModel::class.java)
            setUpUi()
        }
    }

    private fun setUpUi() {
        setUpAqi()
        setUpPollutants()
    }

    private fun setUpPollutants() {
        setPM25()
        setPM10()
        setSO2()
        setNO2()
        setO3()
        setCO()
        setNH3()
    }

    private fun setNH3() {
        val nh3 = approx(placeAirPollutionModel?.list!![0].components.nh3)
        val color = getNOTextColor(nh3)

        binding?.nh3TV?.text = nh3.toString()
        binding?.nh3TV?.setTextColor(color)
    }

    private fun getNOTextColor(nh3: Double): Int {
        return when (nh3) {
            in 0.0..200.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 200.0..400.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 400.0..800.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 800.0..1200.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun setCO() {
        val co = approx(placeAirPollutionModel?.list!![0].components.co)
        val color = getCOTextColor(co)

        binding?.coTV?.text = co.toString()
        binding?.coTV?.setTextColor(color)
    }

    private fun getCOTextColor(co: Double): Int {
        return when (co) {
            in 0.0..1.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 1.0..2.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 2.0..10.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 10.0..17.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun setO3() {
        val o3 = approx(placeAirPollutionModel?.list!![0].components.o3)
        val color = getO3TextColor(o3)

        binding?.o3TV?.text = o3.toString()
        binding?.o3TV?.setTextColor(color)
    }

    private fun getO3TextColor(o3: Double): Int {
        return when (o3) {
            in 0.0..50.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 50.0..100.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 100.0..168.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 168.0..208.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun setNO2() {
        val no2 = approx(placeAirPollutionModel?.list!![0].components.no2)
        val color = getNO2TextColor(no2)

        binding?.no2TV?.text = no2.toString()
        binding?.no2TV?.setTextColor(color)
    }

    private fun getNO2TextColor(no2: Double): Int {
        return when (no2) {
            in 0.0..40.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 40.0..80.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 80.0..180.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 180.0..280.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun setSO2() {
        val so2 = approx(placeAirPollutionModel?.list!![0].components.so2)
        val color = getSO2TextColor(so2)

        binding?.so2TV?.text = so2.toString()
        binding?.so2TV?.setTextColor(color)
    }

    private fun getSO2TextColor(so2: Double): Int {
        return when (so2) {
            in 0.0..40.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 40.0..80.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 80.0..380.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 380.0..800.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun setPM10() {
        val pm10 = approx(placeAirPollutionModel?.list!![0].components.pm10)
        val color = getPM10TextColor(pm10)

        binding?.pm10TV?.text = pm10.toString()
        binding?.pm10TV?.setTextColor(color)
    }

    private fun getPM10TextColor(pm10: Double): Int {
        return when (pm10) {
            in 0.0..50.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 50.0..100.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 100.0..200.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 200.0..300.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun setPM25() {
        val pm25 = approx(placeAirPollutionModel?.list!![0].components.pm2_5)
        val color = getPM25TextColor(pm25)

        binding?.pm25TV?.text = pm25.toString()
        binding?.pm25TV?.setTextColor(color)
    }

    private fun getPM25TextColor(pm25: Double): Int {
        return when (pm25) {
            in 0.0..30.0 -> applicationContext.resources.getColor(R.color.green, null)
            in 30.0..60.0 -> applicationContext.resources.getColor(R.color.yellow, null)
            in 60.0..90.0 -> applicationContext.resources.getColor(R.color.orange, null)
            in 90.0..120.0 -> applicationContext.resources.getColor(R.color.red, null)
            else -> applicationContext.resources.getColor(R.color.purple, null)
        }
    }

    private fun approx(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }

    private fun setUpAqi() {
        val aqi = placeAirPollutionModel?.list!![0].main.aqi
        val color = Objects.setTextColorAsAqi(applicationContext, aqi)

        binding?.aqiLevelTV?.text = aqi.toString()
        binding?.warningTV?.text = getAqiWarning(aqi)
        binding?.descriptionTV?.text = getAqiDescription(aqi)
        binding?.aqiLevelTV?.setTextColor(color)
        binding?.warningTV?.setTextColor(color)
    }

    private fun getAqiDescription(aqi: Int): CharSequence {
        return when (aqi) {
            1 -> applicationContext.resources.getString(R.string.aqi_1_description)
            2 -> applicationContext.resources.getString(R.string.aqi_2_description)
            3 -> applicationContext.resources.getString(R.string.aqi_3_description)
            4 -> applicationContext.resources.getString(R.string.aqi_4_description)
            else -> applicationContext.resources.getString(R.string.aqi_5_description)
        }
    }

    private fun getAqiWarning(aqi: Int): CharSequence {
        return when (aqi) {
            1 -> applicationContext.resources.getString(R.string.aqi_1_warning)
            2 -> applicationContext.resources.getString(R.string.aqi_2_warning)
            3 -> applicationContext.resources.getString(R.string.aqi_3_warning)
            4 -> applicationContext.resources.getString(R.string.aqi_4_warning)
            else -> applicationContext.resources.getString(R.string.aqi_5_warning)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}