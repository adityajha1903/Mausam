package com.example.adi.mausam.activities

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.adi.mausam.Objects
import com.example.adi.mausam.Objects.LOCATION_REQ_CODE
import com.example.adi.mausam.Objects.PLACE_ID
import com.example.adi.mausam.R
import com.example.adi.mausam.activities.SearchActivity.Companion.LOCATION_LATITUDE
import com.example.adi.mausam.activities.SearchActivity.Companion.LOCATION_LONGITUDE
import com.example.adi.mausam.activities.SearchActivity.Companion.LOCATION_NAME
import com.example.adi.mausam.adapters.PlacesRecyclerViewAdapter
import com.example.adi.mausam.api.MyApis
import com.example.adi.mausam.gesturecontrol.ItemMoveCallback
import com.example.adi.mausam.databinding.ActivityMainBinding
import com.example.adi.mausam.databinding.AddNewLocationDialogBinding
import com.example.adi.mausam.db.dao.PlacesDao
import com.example.adi.mausam.db.database.PlacesDatabase
import com.example.adi.mausam.db.entity.PlacesEntity
import com.example.adi.mausam.models.mainresponses.AirPollutionModel
import com.example.adi.mausam.models.mainresponses.CurrentWeatherModel
import com.example.adi.mausam.models.mainresponses.WeatherForecastModel
import com.github.ybq.android.spinkit.style.ThreeBounce
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.Callback
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    private var placesList = ArrayList<PlacesEntity>()
    private var adapter: PlacesRecyclerViewAdapter? = null

    private var addNewLocationDialog: Dialog? = null
    private var addNewLocationDialogBinding: AddNewLocationDialogBinding? = null

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent: Intent? = result.data
            if (intent != null) {
                val name = intent.getStringExtra(LOCATION_NAME)
                val lat = intent.getDoubleExtra(LOCATION_LATITUDE, 10000000.0)
                val lon = intent.getDoubleExtra(LOCATION_LONGITUDE, 10000000.0)
                if (lat != 10000000.0 && lon != 10000000.0) {
                    getDataFromApi(name!!, Objects.roundOff(lat), Objects.roundOff(lon))
                }
            }
        }
    }

    private val startSettingsToTurnOnGps: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isGpsEnabled()) {
                putLanLongInEditText()
            }
        }

    private val startSettingsToGetLocationPermission: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                askPermissionToGetLocation()
            }
        }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.progress?.setIndeterminateDrawable(object: ThreeBounce(){})

        setSupportActionBar(binding?.toolBar)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                backPress()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPress()
                }
            })
        }
        binding?.backPressIB?.setOnClickListener {
            backPress()
        }

        binding?.ssPullToRefreshLayout?.setOnRefreshListener(refreshListener)

        updatePlacesList(true)

        val animTopToBottom = AnimationUtils.loadAnimation(this, R.anim.top_to_bottom_anim)
        binding?.delAllSelectedPlaces?.startAnimation(animTopToBottom)
        binding?.delAllSelectedPlaces?.isClickable = false

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding?.addNewPlacesIB?.setOnClickListener{
            showAddNewLocationDialog()
        }

        binding?.selectMultiplePlacesIB?.setOnClickListener {
            if (PlacesRecyclerViewAdapter.isMultiSelectActive) {
                for (holder in PlacesRecyclerViewAdapter.holderList) {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.item_long_pressed_anim)
                    holder.item.startAnimation(anim)
                    holder.selectBtn.setBackgroundResource(R.drawable.ic_selected_place_bg)
                }
                var i = 0
                while (i < PlacesRecyclerViewAdapter.selectedPlaceList.size) {
                    PlacesRecyclerViewAdapter.selectedPlaceList[i] = true
                    i++
                }
            }
        }

        binding?.delAllSelectedPlaces?.setOnClickListener {
            val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
            val placesDao = placesDb.placesDao()
            val anim = AnimationUtils.loadAnimation(this, R.anim.left_to_right_anim)
            lifecycleScope.launch {
                for ((i, placeSelected) in PlacesRecyclerViewAdapter.selectedPlaceList.withIndex()) {
                    if (placeSelected) {
                        PlacesRecyclerViewAdapter.holderList[i].item.startAnimation(anim)
                        placesDao.delete(placesList[i])
                    }
                }
                delay(500)
                backPress()
            }
        }
    }

    private val refreshListener = object: SSPullToRefreshLayout.OnRefreshListener{
        override fun onRefresh() {
            lifecycleScope.launch {
                refreshDataUsingApi()
            }
        }
    }

    private fun backPress() {
        if (PlacesRecyclerViewAdapter.isMultiSelectActive) {
            val animTopToBottom = AnimationUtils.loadAnimation(this, R.anim.top_to_bottom_anim)
            val animBottomToTop = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top_anim)
            binding?.addNewPlacesIB?.startAnimation(animBottomToTop)
            binding?.addNewPlacesIB?.isClickable = true
            binding?.delAllSelectedPlaces?.startAnimation(animTopToBottom)
            binding?.delAllSelectedPlaces?.isClickable = false
            PlacesRecyclerViewAdapter.isMultiSelectActive = false
            for (holder in PlacesRecyclerViewAdapter.holderList) {
                holder.dragLiver.visibility = View.GONE
                holder.selectBtn.visibility = View.GONE
            }
            PlacesRecyclerViewAdapter.selectedPlaceList = ArrayList()
            PlacesRecyclerViewAdapter.holderList = ArrayList()
            updatePlacesList(false)
            isMultiSelectStateActive(false)
        } else {
            finish()
        }
    }

    private fun updatePlacesList(updateDataToo: Boolean) {
        val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
        val placesDao = placesDb.placesDao()
        lifecycleScope.launch {
            placesDao.fetchAllPlaces().collect {
                placesList = ArrayList(it)
                var i = 0
                while (i < placesList.size) {
                    PlacesRecyclerViewAdapter.selectedPlaceList.add(i, false)
                    i++
                }
                setUpRecyclerView()
                if (updateDataToo) {
                    if (Objects.isInternetAvailable(applicationContext)) {
                        binding?.ll?.visibility = View.VISIBLE
                        refreshDataUsingApi()
                    } else {
                        Toast.makeText(this@MainActivity, "Network not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "UseCompatLoadingForDrawables")
    private fun setUpRecyclerView() {
        if (placesList.isEmpty()) {
            binding?.textViewNote?.visibility = View.VISIBLE
        } else {
            binding?.textViewNote?.visibility = View.INVISIBLE
        }

        adapter = PlacesRecyclerViewAdapter(this, placesList, { position, holder->
            if (PlacesRecyclerViewAdapter.isMultiSelectActive) {
                if (PlacesRecyclerViewAdapter.selectedPlaceList[position]) {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.item_long_press_stoped_anim)
                    holder.item.startAnimation(anim)
                    holder.selectBtn.setBackgroundResource(R.drawable.ic_not_selected_place)
                    PlacesRecyclerViewAdapter.selectedPlaceList[position] = false
                } else {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.item_long_pressed_anim)
                    holder.item.startAnimation(anim)
                    holder.selectBtn.setBackgroundResource(R.drawable.ic_selected_place_bg)
                    PlacesRecyclerViewAdapter.selectedPlaceList[position] = true
                }
            } else {
                val intent = Intent(this, WeatherActivity::class.java)
                intent.putExtra(PLACE_ID, placesList[position].id)
                startActivity(intent)
            }
        },{ view ->
            if (!PlacesRecyclerViewAdapter.isMultiSelectActive) {
                val anim = AnimationUtils.loadAnimation(this, R.anim.item_touch_anim)
                view.startAnimation(anim)
            }
        },{ viewHolder, direction ->
            if (direction == LEFT) {
                val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
                val placesDao = placesDb.placesDao()
                lifecycleScope.launch {
                    placesDao.delete(placesList[viewHolder.bindingAdapterPosition])
                    showUndoSnackBar(placesList[viewHolder.bindingAdapterPosition], placesDao)
                    if (PlacesRecyclerViewAdapter.isMultiSelectActive) {
                        backPress()
                    } else {
                        updatePlacesList(false)
                    }
                }
            }
        }, { c, _, viewHolder, dX, _, actionState, _ ->
            if (actionState == ACTION_STATE_SWIPE) {
                val d = resources.getDrawable(R.drawable.dlt_bg, null)
                d.setBounds(viewHolder.itemView.right + dX.toInt(), viewHolder.itemView.top, viewHolder.itemView.right, viewHolder.itemView.bottom)
                d.draw(c)
            }
        }, { myViewHolder, position ->
            if (!PlacesRecyclerViewAdapter.isMultiSelectActive) {
                val animTopToBottom = AnimationUtils.loadAnimation(this, R.anim.top_to_bottom_anim)
                val animBottomToTop = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top_anim)
                binding?.addNewPlacesIB?.startAnimation(animTopToBottom)
                binding?.addNewPlacesIB?.isClickable = false
                binding?.delAllSelectedPlaces?.startAnimation(animBottomToTop)
                binding?.delAllSelectedPlaces?.isClickable = true
                val anim = AnimationUtils.loadAnimation(this, R.anim.item_long_pressed_anim)
                myViewHolder?.item?.startAnimation(anim)
                PlacesRecyclerViewAdapter.selectedPlaceList[position] = true
                PlacesRecyclerViewAdapter.isMultiSelectActive = true
                isMultiSelectStateActive(PlacesRecyclerViewAdapter.isMultiSelectActive)
                for (holder in PlacesRecyclerViewAdapter.holderList) {
                    holder.dragLiver.visibility = View.VISIBLE
                    holder.selectBtn.visibility = View.VISIBLE
                }
                myViewHolder?.selectBtn?.setBackgroundResource(R.drawable.ic_selected_place_bg)
            }
        })
        val llm = LinearLayoutManager(this)
        binding?.placesRV?.adapter = adapter
        binding?.placesRV?.layoutManager = llm
        ItemTouchHelper(ItemMoveCallback(adapter)).attachToRecyclerView(binding?.placesRV)
    }

    private fun isMultiSelectStateActive(multiSelectActive: Boolean) {
        if (multiSelectActive) {
            binding?.backPressIB?.visibility = View.VISIBLE
            binding?.selectMultiplePlacesIB?.visibility = View.VISIBLE
        } else {
            binding?.backPressIB?.visibility = View.GONE
            binding?.selectMultiplePlacesIB?.visibility = View.GONE
        }
    }

    private fun showUndoSnackBar(placesEntity: PlacesEntity, placesDao: PlacesDao) {
        val snackBar = Snackbar.make(binding?.root!!, "${placesEntity.location} deleted", Snackbar.LENGTH_SHORT)
        snackBar.setAction("Undo") {
            lifecycleScope.launch {
                placesDao.insert(placesEntity)
                PlacesRecyclerViewAdapter.selectedPlaceList = ArrayList()
                PlacesRecyclerViewAdapter.holderList = ArrayList()
                updatePlacesList(false)
            }
        }
        snackBar.view.setBackgroundColor(Color.parseColor("#C0C4C8"))
        snackBar.setActionTextColor(Color.BLUE)
        snackBar.setTextColor(Color.BLACK)
        snackBar.show()
    }


    private fun showAddNewLocationDialog() {
        addNewLocationDialog = Dialog(this)
        addNewLocationDialogBinding = AddNewLocationDialogBinding.inflate(layoutInflater)
        addNewLocationDialog?.setContentView(addNewLocationDialogBinding?.root!!)
        addNewLocationDialogBinding?.cancelIB?.setOnClickListener {
            addNewLocationDialog?.dismiss() }
        addNewLocationDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        addNewLocationDialogBinding?.eTLongitude?.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                val numStr = text.toString()
                if (numStr != "." && numStr != "-" && numStr != "-.") {
                    val num = numStr.toDouble()
                    if (num < -180.0 || num > 180.0) {
                        addNewLocationDialogBinding?.tILongitude?.error = "min = -180.0, max = 180.0"
                    } else {
                        addNewLocationDialogBinding?.tILongitude?.error = null
                    }
                }
            }
        }

        addNewLocationDialogBinding?.eTLatitude?.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                val numStr = text.toString()
                if (numStr != "." && numStr != "-" && numStr != "-.") {
                    val num = numStr.toDouble()
                    if (num < -90.0 || num > 90.0) {
                        addNewLocationDialogBinding?.tILatitude?.error = "min = -90.0, max = 90.0"
                    } else {
                        addNewLocationDialogBinding?.tILatitude?.error = null
                    }
                }
            }
        }

        addNewLocationDialogBinding?.presentLocationIB?.setOnClickListener {
            if(Objects.isInternetAvailable(applicationContext)) {
                askPermissionToGetLocation()
            } else {
                Toast.makeText(this, "Network not available", Toast.LENGTH_SHORT).show()
            }
        }

        addNewLocationDialogBinding?.addLocationToDatabase?.setOnClickListener {
            if (errorIsNotVisible()) {
                if (requiredFieldsNotFilled()) {
                    Toast.makeText(this, "Fill all the required fields", Toast.LENGTH_SHORT).show()
                } else {
                    getDataFromApi(addNewLocationDialogBinding?.eTLocation?.text?.toString()!!, Objects.roundOff(addNewLocationDialogBinding?.eTLatitude?.text?.toString()?.toDouble()!!), Objects.roundOff(addNewLocationDialogBinding?.eTLongitude?.text?.toString()?.toDouble()!!))
                }
            }
            addNewLocationDialog?.dismiss()
        }

        addNewLocationDialogBinding?.search?.setOnClickListener {
            val intent = Intent(this@MainActivity, SearchActivity::class.java)
            resultLauncher.launch(intent)
            addNewLocationDialog?.dismiss()
        }

        addNewLocationDialog?.show()
    }

    private fun refreshDataUsingApi() {
        if (Objects.isInternetAvailable(applicationContext)) {
            val placesDb = Room.databaseBuilder(applicationContext, PlacesDatabase::class.java, "place_database").build()
            val placesDao = placesDb.placesDao()

            val gson = Gson()

            val retrofit = Retrofit.Builder().baseUrl(Objects.BASE_URL_AIR_POLLUTION).addConverterFactory(GsonConverterFactory.create()).build()
            val service: MyApis = retrofit.create(MyApis::class.java)

            val retrofit2 = Retrofit.Builder().baseUrl(Objects.BASE_URL_WEATHER_FORECAST).addConverterFactory(GsonConverterFactory.create()).build()
            val service2: MyApis = retrofit2.create(MyApis::class.java)

            val retrofit3 = Retrofit.Builder().baseUrl(Objects.BASE_URL_CURRENT_WEATHER).addConverterFactory(GsonConverterFactory.create()).build()
            val service3: MyApis = retrofit3.create(MyApis::class.java)

            lifecycleScope.launch {
                for ((index, place) in placesList.withIndex()) {
                    val lat = place.latitude
                    val lon = place.longitude
                    val listCall: Call<AirPollutionModel> = service.getAirPollution(lat, lon, Objects.API_ID)

                    listCall.enqueue(object : Callback<AirPollutionModel> {
                        override fun onResponse(
                            call: Call<AirPollutionModel>,
                            response: Response<AirPollutionModel>
                        ) {
                            val airPollutionModel = response.body()
                            if (response.isSuccessful) {

                                val listCall2: Call<WeatherForecastModel> = service2.getWeatherForecast(lat, lon, Objects.API_ID)
                                listCall2.enqueue(object: Callback<WeatherForecastModel>{
                                    override fun onResponse(
                                        call2: Call<WeatherForecastModel>,
                                        response2: Response<WeatherForecastModel>
                                    ) {
                                        val weatherForecastModel = response2.body()
                                        if (response2.isSuccessful) {

                                            val listCall3: Call<CurrentWeatherModel> = service3.getCurrentWeather(lat, lon, Objects.API_ID)
                                            listCall3.enqueue(object: Callback<CurrentWeatherModel>{
                                                override fun onResponse(
                                                    call3: Call<CurrentWeatherModel>,
                                                    response3: Response<CurrentWeatherModel>
                                                ) {
                                                    val currentWeatherModel = response3.body()
                                                    if (response3.isSuccessful) {
                                                        val airPollutionString = gson.toJson(airPollutionModel)
                                                        val weatherForecastString = gson.toJson(weatherForecastModel)
                                                        val currentWeatherString = gson.toJson(currentWeatherModel)
                                                        val newPlace = PlacesEntity(place.id, place.location, place.latitude, place.longitude, currentWeatherString, airPollutionString, weatherForecastString)
                                                        lifecycleScope.launch {
                                                            placesDao.update(newPlace)
                                                            if (index == placesList.size - 1) {
                                                                updatePlacesList(false)
                                                                if (binding?.ll?.visibility == View.VISIBLE) {
                                                                    binding?.ll?.visibility = View.INVISIBLE
                                                                } else {
                                                                    binding?.ssPullToRefreshLayout?.setRefreshing(false)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        when (response3.code()) {
                                                            400 -> {
                                                                Log.e("ERROR 400", "Bad Connection")
                                                            }
                                                            404 -> {
                                                                Log.e("ERROR 404", "Not Found")
                                                            }
                                                            else -> {
                                                                Log.e("ERROR", response3.message())
                                                            }
                                                        }
                                                        if (binding?.ll?.visibility == View.VISIBLE) {
                                                            binding?.ll?.visibility = View.INVISIBLE
                                                        } else {
                                                            binding?.ssPullToRefreshLayout?.setRefreshing(false)
                                                        }
                                                    }
                                                }

                                                override fun onFailure(
                                                    call3: Call<CurrentWeatherModel>,
                                                    t3: Throwable
                                                ) {
                                                    Log.e("Erorrrrrr", t3.message.toString())
                                                    if (binding?.ll?.visibility == View.VISIBLE) {
                                                        binding?.ll?.visibility = View.INVISIBLE
                                                    } else {
                                                        binding?.ssPullToRefreshLayout?.setRefreshing(false)
                                                    }
                                                }

                                            })
                                        } else {
                                            when (response2.code()) {
                                                400 -> {
                                                    Log.e("ERROR 400", "Bad Connection")
                                                }
                                                404 -> {
                                                    Log.e("ERROR 404", "Not Found")
                                                }
                                                else -> {
                                                    Log.e("ERROR", response2.message())
                                                }
                                            }
                                            if (binding?.ll?.visibility == View.VISIBLE) {
                                                binding?.ll?.visibility = View.INVISIBLE
                                            } else {
                                                binding?.ssPullToRefreshLayout?.setRefreshing(false)
                                            }
                                        }
                                    }

                                    override fun onFailure(
                                        call2: Call<WeatherForecastModel>,
                                        t2: Throwable
                                    ) {
                                        Log.e("Erorrrrrr", t2.message.toString())
                                        if (binding?.ll?.visibility == View.VISIBLE) {
                                            binding?.ll?.visibility = View.INVISIBLE
                                        } else {
                                            binding?.ssPullToRefreshLayout?.setRefreshing(false)
                                        }
                                    }

                                })
                            } else {
                                when (response.code()) {
                                    400 -> {
                                        Log.e("ERROR 400", "Bad Connection")
                                    }
                                    404 -> {
                                        Log.e("ERROR 404", "Not Found")
                                    }
                                    else -> {
                                        Log.e("ERROR", response.message())
                                    }
                                }
                                if (binding?.ll?.visibility == View.VISIBLE) {
                                    binding?.ll?.visibility = View.INVISIBLE
                                } else {
                                    binding?.ssPullToRefreshLayout?.setRefreshing(false)
                                }
                            }
                        }

                        override fun onFailure(call: Call<AirPollutionModel>, t: Throwable) {
                            Log.e("Erorrrrrr", t.message.toString())
                            if (binding?.ll?.visibility == View.VISIBLE) {
                                binding?.ll?.visibility = View.INVISIBLE
                            } else {
                                binding?.ssPullToRefreshLayout?.setRefreshing(false)
                            }
                        }
                    })
                }
                if (placesList.isEmpty()) {
                    if (binding?.ll?.visibility == View.VISIBLE) {
                        binding?.ll?.visibility = View.INVISIBLE
                    } else {
                        binding?.ssPullToRefreshLayout?.setRefreshing(false)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Network not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDataFromApi(name: String, lat: Double, lon: Double) {
        if (Objects.isInternetAvailable(applicationContext)) {
            val gson = Gson()
            PlacesRecyclerViewAdapter.selectedPlaceList = ArrayList()
            PlacesRecyclerViewAdapter.holderList = ArrayList()
            updatePlacesList(false)
            val placesDb = Room.databaseBuilder(
                applicationContext,
                PlacesDatabase::class.java,
                "place_database"
            ).build()
            val placesDao = placesDb.placesDao()
            lifecycleScope.launch {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Objects.BASE_URL_AIR_POLLUTION)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val service: MyApis = retrofit.create(MyApis::class.java)
                val listCall: Call<AirPollutionModel> = service.getAirPollution(lat, lon, Objects.API_ID)

                binding?.ll?.visibility = View.VISIBLE
                listCall.enqueue(object : Callback<AirPollutionModel> {
                    override fun onResponse(
                        call: Call<AirPollutionModel>,
                        response: Response<AirPollutionModel>
                    ) {
                        val airPollutionModel = response.body()
                        if (response.isSuccessful) {
                            val retrofit2 = Retrofit.Builder()
                                .baseUrl(Objects.BASE_URL_WEATHER_FORECAST)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                            val service2: MyApis = retrofit2.create(MyApis::class.java)
                            val listCall2: Call<WeatherForecastModel> = service2.getWeatherForecast(lat, lon, Objects.API_ID)
                            listCall2.enqueue(object: Callback<WeatherForecastModel>{
                                override fun onResponse(
                                    call2: Call<WeatherForecastModel>,
                                    response2: Response<WeatherForecastModel>
                                ) {
                                    val weatherForecastModel = response2.body()
                                    if (response2.isSuccessful) {

                                        val retrofit3 = Retrofit.Builder()
                                            .baseUrl(Objects.BASE_URL_CURRENT_WEATHER)
                                            .addConverterFactory(GsonConverterFactory.create())
                                            .build()
                                        val service3: MyApis = retrofit3.create(MyApis::class.java)
                                        val listCall3: Call<CurrentWeatherModel> = service3.getCurrentWeather(lat, lon, Objects.API_ID)
                                        listCall3.enqueue(object: Callback<CurrentWeatherModel>{
                                            override fun onResponse(
                                                call3: Call<CurrentWeatherModel>,
                                                response3: Response<CurrentWeatherModel>
                                            ) {
                                                val currentWeatherModel = response3.body()
                                                if (response3.isSuccessful) {
                                                    val airPollutionString = gson.toJson(airPollutionModel)
                                                    val weatherForecastString = gson.toJson(weatherForecastModel)
                                                    val currentWeatherString = gson.toJson(currentWeatherModel)
                                                    lifecycleScope.launch {
                                                        placesDao.insert(
                                                            PlacesEntity(location = name, latitude = lat, longitude = lon, currentWeatherData = currentWeatherString, aqi = airPollutionString, fiveDayWeatherData = weatherForecastString)
                                                        )
                                                        PlacesRecyclerViewAdapter.selectedPlaceList = ArrayList()
                                                        PlacesRecyclerViewAdapter.holderList = ArrayList()
                                                        updatePlacesList(false)
                                                        if (binding?.ll?.visibility == View.VISIBLE) {
                                                            binding?.ll?.visibility = View.INVISIBLE
                                                        }
                                                    }
                                                } else {
                                                    when (response2.code()) {
                                                        400 -> {
                                                            Log.e("ERROR 400", "Bad Connection")
                                                        }
                                                        404 -> {
                                                            Log.e("ERROR 404", "Not Found")
                                                        }
                                                        else -> {
                                                            Log.e("ERROR", response2.message())
                                                        }
                                                    }
                                                    if (binding?.ll?.visibility == View.VISIBLE) {
                                                        binding?.ll?.visibility = View.INVISIBLE
                                                    }
                                                }
                                            }

                                            override fun onFailure(
                                                call3: Call<CurrentWeatherModel>,
                                                t3: Throwable
                                            ) {
                                                Log.e("Erorrrrrr", t3.message.toString())
                                                if (binding?.ll?.visibility == View.VISIBLE) {
                                                    binding?.ll?.visibility = View.INVISIBLE
                                                }
                                            }

                                        })
                                    } else {
                                        when (response2.code()) {
                                            400 -> {
                                                Log.e("ERROR 400", "Bad Connection")
                                            }
                                            404 -> {
                                                Log.e("ERROR 404", "Not Found")
                                            }
                                            else -> {
                                                Log.e("ERROR", response2.message())
                                            }
                                        }
                                        if (binding?.ll?.visibility == View.VISIBLE) {
                                            binding?.ll?.visibility = View.INVISIBLE
                                        }
                                    }
                                }

                                override fun onFailure(
                                    call2: Call<WeatherForecastModel>,
                                    t2: Throwable
                                ) {
                                    Log.e("Erorrrrrr", t2.message.toString())
                                    if (binding?.ll?.visibility == View.VISIBLE) {
                                        binding?.ll?.visibility = View.INVISIBLE
                                    }
                                }

                            })
                        } else {
                            when (response.code()) {
                                400 -> {
                                    Log.e("ERROR 400", "Bad Connection")
                                }
                                404 -> {
                                    Log.e("ERROR 404", "Not Found")
                                }
                                else -> {
                                    Log.e("ERROR", response.message())
                                }
                            }
                            if (binding?.ll?.visibility == View.VISIBLE) {
                                binding?.ll?.visibility = View.INVISIBLE
                            }
                        }
                    }

                    override fun onFailure(call: Call<AirPollutionModel>, t: Throwable) {
                        Log.e("Erorrrrrr", t.message.toString())
                        if (binding?.ll?.visibility == View.VISIBLE) {
                            binding?.ll?.visibility = View.INVISIBLE
                        }
                    }
                })
            }
        } else {
            Toast.makeText(this, "Network not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun errorIsNotVisible() =
        addNewLocationDialogBinding?.tILatitude?.error.isNullOrEmpty() && addNewLocationDialogBinding?.tILongitude?.error.isNullOrEmpty()

    private fun requiredFieldsNotFilled(): Boolean {
        return addNewLocationDialogBinding?.eTLatitude?.text.isNullOrEmpty() || addNewLocationDialogBinding?.eTLongitude?.text.isNullOrEmpty() || addNewLocationDialogBinding?.eTLocation?.text.isNullOrEmpty() || addNewLocationDialogBinding?.eTLongitude?.text?.toString() == "-" || addNewLocationDialogBinding?.eTLongitude?.text?.toString() == "-" || addNewLocationDialogBinding?.eTLatitude?.text?.toString() == "-" || addNewLocationDialogBinding?.eTLatitude?.text?.toString() == "."
    }

    private fun askPermissionToGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isGpsEnabled()) {
                putLanLongInEditText()
            } else {
                askUserToTurnOnGps()
            }
        } else {
            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                showRequestPermissionRationaleSnackBar()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), LOCATION_REQ_CODE)
            }
        }
    }

    private fun putLanLongInEditText() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient?.lastLocation
                ?.addOnCompleteListener { task ->
                    val location = task.result
                    if (location != null) {
                        addNewLocationDialogBinding?.eTLatitude?.setText(Objects.roundOff(location.latitude).toString())
                        addNewLocationDialogBinding?.eTLongitude?.setText(Objects.roundOff(location.longitude).toString())
                    } else {
                        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).apply{
                            setWaitForAccurateLocation(true)
                            setMinUpdateIntervalMillis(500)
                            setMaxUpdateDelayMillis(1000)
                        }.build()
                        fusedLocationProviderClient?.requestLocationUpdates(locationRequest!!, locationCallback, Looper.getMainLooper())
                    }
                }
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                addNewLocationDialogBinding?.eTLatitude?.setText(Objects.roundOff(location.latitude).toString())
                addNewLocationDialogBinding?.eTLongitude?.setText(Objects.roundOff(location.longitude).toString())
            }
        }
    }

    private fun showRequestPermissionRationaleSnackBar() {
        val snackBar = Snackbar.make(addNewLocationDialogBinding?.presentLocationIB!!, "Allow location permission", Snackbar.LENGTH_SHORT)
        snackBar.setAction("Settings") {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startSettingsToGetLocationPermission.launch(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
        snackBar.view.setBackgroundColor(Color.parseColor("#C0C4C8"))
        snackBar.setActionTextColor(Color.BLUE)
        snackBar.setTextColor(Color.BLACK)
        snackBar.show()
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun askUserToTurnOnGps() {
        val snackBar = Snackbar.make(addNewLocationDialogBinding?.presentLocationIB!!, "Enable device location", Snackbar.LENGTH_SHORT)
        snackBar.setAction("Settings") {
                startSettingsToTurnOnGps.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        snackBar.view.setBackgroundColor(Color.parseColor("#C0C4C8"))
        snackBar.setActionTextColor(Color.BLUE)
        snackBar.setTextColor(Color.BLACK)
        snackBar.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQ_CODE -> {
                if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    askPermissionToGetLocation()
                } else {
                    showRequestPermissionRationaleSnackBar()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        addNewLocationDialogBinding = null
    }


}