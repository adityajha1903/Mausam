package com.example.adi.mausam.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.adi.mausam.Objects
import com.example.adi.mausam.Objects.PLACE_ID
import com.example.adi.mausam.R
import com.example.adi.mausam.adapters.ThreeHourForecastAdapter
import com.example.adi.mausam.databinding.ActivityWeatherBinding
import com.example.adi.mausam.db.database.PlacesDatabase
import com.example.adi.mausam.db.entity.PlacesEntity
import com.example.adi.mausam.models.mainresponses.AirPollutionModel
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel
import com.example.adi.mausam.models.mainresponses.WeatherForecastModel
import com.google.gson.Gson
import kotlinx.coroutines.launch

class WeatherActivity : AppCompatActivity() {

    private var binding: ActivityWeatherBinding? = null

    private var place: PlacesEntity? = null
    private var placeAirPollutionModel: AirPollutionModel? = null
    private var placeWeatherForecastModel: WeatherForecastModel? = null
    private var placeCurrentWeatherModel: CurrentWeatherModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.weatherActivityToolBar)
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
        binding?.pressBackIB?.setOnClickListener {
            finish()
        }

        val placeId = intent.getIntExtra(PLACE_ID, 0)
        val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
        val placesDao = placesDb.placesDao()
        val gson = Gson()
        lifecycleScope.launch {
            place = placesDao.getPlaceById(placeId)
            placeAirPollutionModel = gson.fromJson(place?.aqi, AirPollutionModel::class.java)
            placeWeatherForecastModel = gson.fromJson(place?.fiveDayWeatherData, WeatherForecastModel::class.java)
            placeCurrentWeatherModel = gson.fromJson(place?.currentWeatherData, CurrentWeatherModel::class.java)
            setUpUi()
        }

        binding?.cl2?.setOnClickListener {
            val intent = Intent(this, AirQualityDisplayActivity::class.java)
            intent.putExtra(PLACE_ID, placeId)
            startActivity(intent)
        }

        binding?.getFiveDayWeatherForecast?.setOnClickListener {
            val intent = Intent(this, WeatherForecastActivity::class.java)
            intent.putExtra(PLACE_ID, placeId)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUpUi() {
        setUpThreeHourForecastRv()
        binding?.placeNameTV?.text = place?.location
        binding?.tempTv?.text = Objects.kelvinToCelcius(placeCurrentWeatherModel?.main?.temp!!).toString()
        binding?.weatherConditionTV?.text = placeCurrentWeatherModel?.weather!![0].main
        binding?.weatherConditionIconIV?.setImageDrawable(Objects.setTempIcon(applicationContext, placeCurrentWeatherModel?.weather!![0].icon, placeCurrentWeatherModel?.weather!![0].id))
        binding?.bg?.setBackgroundColor(Objects.getRvHolderColor(applicationContext, placeCurrentWeatherModel?.weather!![0].main, placeCurrentWeatherModel?.weather!![0].icon))
        binding?.realFeelTempTV?.text = Objects.kelvinToCelcius(placeCurrentWeatherModel?.main?.feels_like!!).toString()
        binding?.sunriseTimeTV?.text = Objects.formatTime(placeWeatherForecastModel?.city?.sunrise!!).subSequence(11, 16)
        binding?.sunsetTimeTV?.text = Objects.formatTime(placeWeatherForecastModel?.city?.sunset!!).subSequence(11, 16)
        val presentTime = System.currentTimeMillis()/1000
        val sunrise = placeWeatherForecastModel?.city?.sunrise!!
        val sunset = placeWeatherForecastModel?.city?.sunset!!
        if (presentTime >= sunset) {
            binding?.progressSunriseSunset?.progress = 100
        } else {
            if (presentTime <= sunrise) {
                binding?.progressSunriseSunset?.progress = 0
            } else {
                binding?.progressSunriseSunset?.progress = (((presentTime - sunrise).toDouble()/(sunset - sunrise))*100.0).toInt()
            }
        }
        binding?.aqiTV?.text = placeAirPollutionModel?.list!![0].main.aqi.toString()
        binding?.aqiTV?.setTextColor(Objects.setTextColorAsAqi(applicationContext, placeAirPollutionModel?.list!![0].main.aqi))
        binding?.aqiIconIV?.setImageDrawable(Objects.getAqiIcon(applicationContext, placeAirPollutionModel?.list!![0].main.aqi))
        binding?.humidityTV?.text = placeCurrentWeatherModel?.main?.humidity.toString()
        binding?.windSpeedTv?.text = Objects.msIntoKh(placeCurrentWeatherModel?.wind?.speed!!).toString()
        binding?.pressureTv?.text = placeCurrentWeatherModel?.main?.pressure.toString()
        binding?.visibilityTv?.text = placeCurrentWeatherModel?.visibility.toString()
        binding?.cloudinessTv?.text = placeCurrentWeatherModel?.clouds?.all.toString()
        binding?.todayTempMinTV?.text = Objects.kelvinToCelcius(Objects.getMinMaxTemp(1, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)[0]).toString()
        binding?.todayTempMaxTV?.text = Objects.kelvinToCelcius(Objects.getMinMaxTemp(1, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)[1]).toString()
        binding?.secondDayTempMinTV?.text = Objects.kelvinToCelcius(Objects.getMinMaxTemp(2, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)[0]).toString()
        binding?.secondDayTempMaxTV?.text = Objects.kelvinToCelcius(Objects.getMinMaxTemp(2, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)[1]).toString()
        binding?.thirdDayTempMinTV?.text = Objects.kelvinToCelcius(Objects.getMinMaxTemp(3, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)[0]).toString()
        binding?.thirdDayTempMaxTV?.text = Objects.kelvinToCelcius(Objects.getMinMaxTemp(3, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)[1]).toString()
        binding?.todayWeatherIconIV?.setImageDrawable(Objects.setTempIcon(applicationContext, placeCurrentWeatherModel?.weather!![0].icon, placeCurrentWeatherModel?.weather!![0].id))
        binding?.todayWeatherConditionTV?.text = binding?.weatherConditionTV?.text

        val secondDayMiddleIndex = Objects.getMiddleIndexOfEveryDay(2, placeWeatherForecastModel?.list!!)
        val thirdDayMiddleIndex = Objects.getMiddleIndexOfEveryDay(3, placeWeatherForecastModel?.list!!)
        binding?.secondDayIconIV?.setImageDrawable(Objects.setTempIcon(applicationContext, placeWeatherForecastModel?.list!![secondDayMiddleIndex].weather[0].icon, placeWeatherForecastModel?.list!![secondDayMiddleIndex].weather[0].id))
        binding?.thirdDayIconIV?.setImageDrawable(Objects.setTempIcon(applicationContext, placeWeatherForecastModel?.list!![thirdDayMiddleIndex].weather[0].icon, placeWeatherForecastModel?.list!![thirdDayMiddleIndex].weather[0].id))
        binding?.secondDayWeatherConditionTV?.text = placeWeatherForecastModel?.list!![secondDayMiddleIndex].weather[0].main
        binding?.thirdDayWeatherConditionTV?.text = placeWeatherForecastModel?.list!![thirdDayMiddleIndex].weather[0].main
        binding?.secondDayNameTV?.text = Objects.getDayName(placeWeatherForecastModel?.list!![secondDayMiddleIndex].dt) + "-"
        binding?.thirdDayNameTV?.text = Objects.getDayName(placeWeatherForecastModel?.list!![thirdDayMiddleIndex].dt) + "-"
    }

    private fun setUpThreeHourForecastRv() {
        val llm = LinearLayoutManager(this)
        llm.orientation = LinearLayoutManager.HORIZONTAL
        binding?.threeHourForecastRv?.adapter =
            ThreeHourForecastAdapter(applicationContext, placeWeatherForecastModel?.list!!, placeCurrentWeatherModel!!)
        binding?.threeHourForecastRv?.layoutManager = llm
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}