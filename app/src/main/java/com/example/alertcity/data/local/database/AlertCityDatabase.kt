package com.example.alertcity.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.alertcity.data.local.converter.ReporteConverters
import com.example.alertcity.data.local.dao.ReporteDao
import com.example.alertcity.data.local.entity.ReporteEntity
import com.example.alertcity.data.local.entity.UsuarioEntity

@Database(entities = [ReporteEntity::class, UsuarioEntity::class], version = 1, exportSchema = false)
@TypeConverters(ReporteConverters::class)
abstract class AlertCityDatabase : RoomDatabase() {
    abstract fun reporteDao(): ReporteDao

    companion object{
        @Volatile
        private var INSTANCE : AlertCityDatabase? = null

        fun getDatabase(context: Context): AlertCityDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlertCityDatabase::class.java,
                    "alert_city_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}