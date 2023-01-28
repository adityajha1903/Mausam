package com.example.adi.mausam.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adi.mausam.Objects
import com.example.adi.mausam.adapters.SearchResultRecyclerAdapter
import com.example.adi.mausam.databinding.ActivitySearchBinding
import com.example.adi.mausam.db.entity.PlacesEntity

class SearchActivity : AppCompatActivity() {

    private var binding: ActivitySearchBinding? = null

    private var availablePlaceList = ArrayList<PlacesEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding?.root)

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

        availablePlaceList = Objects.getAllAvailablePlaces()

        binding?.editText?.doOnTextChanged { text, _, _, _ ->
            updateRecyclerView(text)
        }
    }

    private fun updateRecyclerView(text: CharSequence?) {
        val searchPlaceList = ArrayList<PlacesEntity>()

        for (place in availablePlaceList) {
            if (text?.toString()?.lowercase()?.let { place.location.lowercase().contains(it.lowercase()) } == true) {
                searchPlaceList.add(place)
            }
        }

        if (searchPlaceList.isEmpty()) {
            binding?.warningTV?.visibility = View.VISIBLE
        } else {
            binding?.warningTV?.visibility = View.GONE
        }

        val adapter = SearchResultRecyclerAdapter(searchPlaceList) { place ->
            val intent = Intent()
            intent.putExtra(LOCATION_LATITUDE, Objects.roundOff(place.latitude))
            intent.putExtra(LOCATION_LONGITUDE, Objects.roundOff(place.longitude))
            intent.putExtra(LOCATION_NAME, place.location)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        val llm = LinearLayoutManager(this)
        binding?.recyclerView?.adapter = adapter
        binding?.recyclerView?.layoutManager = llm
    }

    companion object {
        const val LOCATION_LATITUDE = "location_latitude"
        const val LOCATION_LONGITUDE = "location_longitude"
        const val LOCATION_NAME = "location_name"
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}