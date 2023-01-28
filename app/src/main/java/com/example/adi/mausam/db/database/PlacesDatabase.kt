package com.example.adi.mausam.db.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.adi.mausam.db.entity.PlacesEntity
import com.example.adi.mausam.db.dao.PlacesDao

@Database(entities = [PlacesEntity::class], version = 1)
abstract class PlacesDatabase: RoomDatabase() {

    abstract fun placesDao(): PlacesDao

    companion object {
        @Volatile
        private var INSTANCE: PlacesDatabase? = null
        private const val PLACES_DB_NAME = "place_database"

        fun getInstance(context: Context): PlacesDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext, PlacesDatabase::class.java, PLACES_DB_NAME)
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}