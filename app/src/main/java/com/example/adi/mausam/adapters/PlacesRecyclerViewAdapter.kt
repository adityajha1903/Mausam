package com.example.adi.mausam.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adi.mausam.Objects
import com.example.adi.mausam.gesturecontrol.ItemMoveCallback
import com.example.adi.mausam.db.entity.PlacesEntity
import com.example.adi.mausam.databinding.IcPlacesRecyclerViewItemBinding
import com.example.adi.mausam.models.mainresponses.AirPollutionModel
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel
import com.example.adi.mausam.models.mainresponses.WeatherForecastModel
import com.google.gson.Gson
import kotlin.collections.ArrayList

class PlacesRecyclerViewAdapter(
    private val context: Context,
    private val placesList: ArrayList<PlacesEntity>,
    private val itemClickListener: (position: Int, holder: PlacesViewHolder) -> Unit,
    private val itemTouchListener: (view: View) -> Unit,
    private val itemSwiped: (viewHolder: RecyclerView.ViewHolder, direction: Int) -> Unit,
    private val itemSwipeDraw: (c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) -> Unit,
    private val itemLongClickListener: (myViewHolder: PlacesViewHolder?, position: Int) -> Unit
): RecyclerView.Adapter<PlacesRecyclerViewAdapter.PlacesViewHolder>(),
    ItemMoveCallback.ItemTouchHelperContract {

    companion object {
        var isMultiSelectActive = false
        var selectedPlaceList = ArrayList<Boolean>()
        var holderList = ArrayList<PlacesViewHolder>()
    }

    class PlacesViewHolder(binding: IcPlacesRecyclerViewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val item = binding.item
        val placeName = binding.placeNameTv
        val dragLiver = binding.dragLiverIV
        val placeAqi = binding.aqiTV
        val placeMinTemp = binding.minTempTv
        val placeMaxTemp = binding.maxTempTv
        val placeTemp = binding.temperatureTV
        val selectBtn = binding.selectPlaceIB
        val itemBg = binding.itemBg
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
        return PlacesViewHolder(
            IcPlacesRecyclerViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    private var startTime: Long = 0
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlacesViewHolder, position: Int) {
        val place = placesList[position]

        if (isMultiSelectActive) {
            holder.dragLiver.visibility = View.VISIBLE
            holder.selectBtn.visibility = View.VISIBLE
        } else {
            holder.dragLiver.visibility = View.GONE
            holder.selectBtn.visibility = View.GONE
        }
        holderList.add(holder)
        holder.placeName.text = place.location

        val gson = Gson()
        if (place.aqi.isNotEmpty()) {
            val airPollutionModel = gson.fromJson(place.aqi, AirPollutionModel::class.java)
            holder.placeAqi.text = airPollutionModel.list[0].main.aqi.toString()
        }

        if (place.currentWeatherData.isNotEmpty()) {
            val currentWeatherModel = gson.fromJson(place.currentWeatherData, CurrentWeatherModel::class.java)
            holder.placeTemp.text = Objects.kelvinToCelcius(currentWeatherModel.main.temp).toString()
            holder.itemBg.setBackgroundColor(Objects.getRvHolderColor(context, currentWeatherModel.weather[0].main, currentWeatherModel.weather[0].icon))

            if (place.fiveDayWeatherData.isNotEmpty()) {
                val weatherForecastModel = gson.fromJson(place.fiveDayWeatherData, WeatherForecastModel::class.java)
                val minMaxTemp = Objects.getMinMaxTemp(1, weatherForecastModel.list, currentWeatherModel)
                holder.placeMinTemp.text = Objects.kelvinToCelcius(minMaxTemp[0]).toString()
                holder.placeMaxTemp.text = Objects.kelvinToCelcius(minMaxTemp[1]).toString()
            }
        }

        holder.item.setOnLongClickListener {
            itemLongClickListener.invoke(holder, position)
            true
        }

        holder.item.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    itemTouchListener.invoke(view)
                    startTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - startTime < 500) {
                        itemClickListener.invoke(position, holder)
                    }
                }
            }
            false
        }
    }

    override fun getItemCount(): Int {
        return placesList.size
    }

    override fun onRowSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        itemSwiped.invoke(viewHolder, direction)
    }

    override fun onRowSwipeDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        itemSwipeDraw.invoke(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}