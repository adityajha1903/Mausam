package com.example.adi.mausam.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adi.mausam.Objects
import com.example.adi.mausam.databinding.IcEveryThreeHourForecastItemBinding
import com.example.adi.mausam.models.internalresponses.EveryThreeHourForecast
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel

class ThreeHourForecastAdapter(private val context: Context, private val forecastList: List<EveryThreeHourForecast>, private val placeCurrentWeatherModel: CurrentWeatherModel): RecyclerView.Adapter<ThreeHourForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(binding: IcEveryThreeHourForecastItemBinding): RecyclerView.ViewHolder(binding.root) {
        var timeTv = binding.timeTv
        var forecastTemp = binding.forecastTempTv
        var icon = binding.forecastIconIV
        var windSpeed = binding.forecastWindSpeed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        return ForecastViewHolder(IcEveryThreeHourForecastItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        if (position == 0) {
            holder.timeTv.text = "Now"
            holder.forecastTemp.text = Objects.kelvinToCelcius(placeCurrentWeatherModel.main.temp).toString()
            holder.icon.setImageDrawable(Objects.setTempIcon(context, placeCurrentWeatherModel.weather[0].icon, placeCurrentWeatherModel.weather[0].id))
            holder.windSpeed.text = Objects.msIntoKh(placeCurrentWeatherModel.wind.speed).toString()
        } else {
            val forecast = forecastList[position - 1]
            holder.timeTv.text = Objects.formatTime(forecast.dt).subSequence(11, 16)
            holder.forecastTemp.text = Objects.kelvinToCelcius(forecast.main.temp).toString()
            holder.icon.setImageDrawable(Objects.setTempIcon(context, forecast.weather[0].icon, forecast.weather[0].id))
            holder.windSpeed.text = Objects.msIntoKh(forecast.wind.speed).toString()
        }
    }

    override fun getItemCount(): Int {
        return forecastList.size + 1
    }
}