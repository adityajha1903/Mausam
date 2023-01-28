package com.example.adi.mausam.activities

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.adi.mausam.Objects
import com.example.adi.mausam.R
import com.example.adi.mausam.customview.DataPoint
import com.example.adi.mausam.databinding.ActivityWeatherForecastBinding
import com.example.adi.mausam.db.database.PlacesDatabase
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel
import com.example.adi.mausam.models.mainresponses.WeatherForecastModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class WeatherForecastActivity : AppCompatActivity() {

    private var binding: ActivityWeatherForecastBinding? = null

    private var placeWeatherForecastModel: WeatherForecastModel? = null
    private var placeCurrentWeatherModel: CurrentWeatherModel? = null

    private var dayNameList = ArrayList<TextView>()
    private var dateList = ArrayList<TextView>()
    private var tempMinList = ArrayList<TextView>()
    private var tempMaxList = ArrayList<TextView>()
    private var iconList = ArrayList<ImageView>()
    private var windSpeedList = ArrayList<TextView>()

    private var tempList = ArrayList<DataPoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_forecast)

        binding = ActivityWeatherForecastBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.weatherForecastToolBar)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
        }
        binding?.backPressIB3?.setOnClickListener {
            finish()
        }

        addAllList()
        fetchDataFromDatabase()
    }

    private fun addAllList() {
        dayNameList.add(binding?.dayName1!!)
        dayNameList.add(binding?.dayName2!!)
        dayNameList.add(binding?.dayName3!!)
        dayNameList.add(binding?.dayName4!!)
        dayNameList.add(binding?.dayName5!!)

        dateList.add(binding?.dateTV1!!)
        dateList.add(binding?.dateTV2!!)
        dateList.add(binding?.dateTV3!!)
        dateList.add(binding?.dateTV4!!)
        dateList.add(binding?.dateTV5!!)

        tempMinList.add(binding?.minTempTv1!!)
        tempMinList.add(binding?.minTempTv2!!)
        tempMinList.add(binding?.minTempTv3!!)
        tempMinList.add(binding?.minTempTv4!!)
        tempMinList.add(binding?.minTempTv5!!)

        tempMaxList.add(binding?.maxTempTv1!!)
        tempMaxList.add(binding?.maxTempTv2!!)
        tempMaxList.add(binding?.maxTempTv3!!)
        tempMaxList.add(binding?.maxTempTv4!!)
        tempMaxList.add(binding?.maxTempTv5!!)

        iconList.add(binding?.iconDay1!!)
        iconList.add(binding?.iconDay2!!)
        iconList.add(binding?.iconDay3!!)
        iconList.add(binding?.iconDay4!!)
        iconList.add(binding?.iconDay5!!)

        windSpeedList.add(binding?.windSpeedDay1!!)
        windSpeedList.add(binding?.windSpeedDay2!!)
        windSpeedList.add(binding?.windSpeedDay3!!)
        windSpeedList.add(binding?.windSpeedDay4!!)
        windSpeedList.add(binding?.windSpeedDay5!!)
    }

    private fun fetchDataFromDatabase() {
        val placeId = intent.getIntExtra(Objects.PLACE_ID, 0)
        val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
        val placesDao = placesDb.placesDao()
        val gson = Gson()
        lifecycleScope.launch {
            val place = placesDao.getPlaceById(placeId)
            placeWeatherForecastModel = gson.fromJson(place.fiveDayWeatherData, WeatherForecastModel::class.java)
            placeCurrentWeatherModel = gson.fromJson(place.currentWeatherData, CurrentWeatherModel::class.java)
            setUpUi()
        }
    }

    private fun setUpUi() {
        var i = 0
        while (i < 5) {
            val minMaxTemp = Objects.getMinMaxTemp(i + 1,placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)
            val minTemp = Objects.kelvinToCelcius(minMaxTemp[0])
            val maxTemp = Objects.kelvinToCelcius(minMaxTemp[1])
            tempList.add(DataPoint(minTemp, maxTemp))
            tempMinList[i].text = minTemp.toString()
            tempMaxList[i].text = maxTemp.toString()
            val middleIndex = Objects.getMiddleIndexOfEveryDay(i + 1, placeWeatherForecastModel?.list!!)
            if (i == 0) {
                dateList[i].text = Objects.formatTime(System.currentTimeMillis() / 1000).subSequence(0, 5)
                iconList[i].setImageDrawable(Objects.setTempIcon(applicationContext, placeCurrentWeatherModel?.weather!![0].icon, placeCurrentWeatherModel?.weather!![0].id))
                windSpeedList[i].text = oneDecimal(Objects.msIntoKh(placeCurrentWeatherModel?.wind?.speed!!)).toString()
            } else {
                dayNameList[i].text = getFullDayName(placeWeatherForecastModel?.list!![middleIndex].dt)
                dateList[i].text = Objects.formatTime(placeWeatherForecastModel?.list!![middleIndex].dt).subSequence(0, 5)
                iconList[i].setImageDrawable(Objects.setTempIcon(applicationContext, placeWeatherForecastModel?.list!![middleIndex].weather[0].icon, placeWeatherForecastModel?.list!![middleIndex].weather[0].id))
                windSpeedList[i].text = oneDecimal(Objects.msIntoKh(placeWeatherForecastModel?.list!![middleIndex].wind.speed)).toString()
            }
            i++
        }

        binding?.graph?.setData(tempList)
    }

    @SuppressLint("SimpleDateFormat")
    fun getFullDayName(unix: Long): String {
        val unixIst = unix + 19800
        val date = Date(unixIst * 1000)
        val simpleDateFormat = SimpleDateFormat("EEEE")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("IST")
        return simpleDateFormat.format(date)
    }

    private fun oneDecimal(num: Float): Float {
        return (num * 10.0f).roundToInt() / 10.0f
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}