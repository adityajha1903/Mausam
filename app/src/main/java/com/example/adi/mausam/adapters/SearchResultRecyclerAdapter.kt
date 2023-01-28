package com.example.adi.mausam.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adi.mausam.databinding.SearchRecyclerViewHolderBinding
import com.example.adi.mausam.db.entity.PlacesEntity

class SearchResultRecyclerAdapter(
    private val placeResultList: ArrayList<PlacesEntity>,
    private val placeOnClickListener: (place: PlacesEntity) -> Unit
): RecyclerView.Adapter<SearchResultRecyclerAdapter.SearchViewHolder>() {

    class SearchViewHolder(binding: SearchRecyclerViewHolderBinding): RecyclerView.ViewHolder(binding.root) {
        val view = binding.root
        val name = binding.placeNameTV
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(SearchRecyclerViewHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val place = placeResultList[position]
        holder.name.text = place.location
        holder.view.setOnClickListener {
            placeOnClickListener.invoke(place)
        }
    }

    override fun getItemCount(): Int {
        return placeResultList.size
    }

}