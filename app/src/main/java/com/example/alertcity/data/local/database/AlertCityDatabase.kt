package com.example.alertcity.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.alertcity.data.local.converter.ReporteConverters
import com.example.alertcity.data.local.dao.ReporteDao
import com.example.alertcity.data.local.dao.UsuarioDao
import com.example.alertcity.data.local.entity.ReporteEntity
import com.example.alertcity.data.local.entity.UsuarioEntity

@Database(
    entities = [ReporteEntity::class, UsuarioEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(ReporteConverters::class)
abstract class AlertCityDatabase : RoomDatabase() {
    abstract fun reporteDao(): ReporteDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: AlertCityDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reportes ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reportes ADD COLUMN latitud REAL")
                db.execSQL("ALTER TABLE reportes ADD COLUMN longitud REAL")
                db.execSQL("ALTER TABLE reportes ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE reportes ADD COLUMN localFotoUri TEXT")
                db.execSQL("ALTER TABLE reportes ADD COLUMN syncError TEXT")
            }
        }

        fun getDatabase(context: Context): AlertCityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlertCityDatabase::class.java,
                    "alert_city_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
