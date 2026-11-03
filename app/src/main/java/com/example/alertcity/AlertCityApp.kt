package com.example.alertcity

import android.app.Application
import com.example.alertcity.data.local.database.AlertCityDatabase
import com.example.alertcity.data.remote.ApiClient
import com.example.alertcity.data.repository.NetworkBoundReporteRepository
import com.example.alertcity.domain.repository.ReporteRepository

class AlertCityApp : Application() {
    val database by lazy { AlertCityDatabase.getDatabase(this) }
    val reporteRepository: ReporteRepository by lazy {
        NetworkBoundReporteRepository(
            dao = database.reporteDao(),
            api = ApiClient.api
        )
    }
}
