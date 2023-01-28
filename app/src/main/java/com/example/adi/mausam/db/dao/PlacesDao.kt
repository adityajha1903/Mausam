package com.example.adi.mausam.db.dao

import androidx.room.*
import com.example.adi.mausam.db.entity.PlacesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacesDao {

    @Query("select * from `placesTable`")
    fun fetchAllPlaces(): Flow<List<PlacesEntity>>

    @Insert
    suspend fun insert(placeEntity: PlacesEntity)

    @Delete
    suspend fun delete(placeEntity: PlacesEntity)

    @Update
    suspend fun update(placeEntity: PlacesEntity)

    @Query("select * from `placesTable` where id = :id")
    suspend fun getPlaceById(id: Int): PlacesEntity
}