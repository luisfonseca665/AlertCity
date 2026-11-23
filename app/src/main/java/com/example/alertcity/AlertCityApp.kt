package com.example.alertcity

import android.app.Application
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.local.database.AlertCityDatabase
import com.example.alertcity.data.remote.ApiClient
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.data.remote.NetworkMonitor
import com.example.alertcity.data.repository.AuthRepository
import com.example.alertcity.data.repository.NetworkBoundReporteRepository
import com.example.alertcity.data.worker.SyncScheduler
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.ui.components.configurarOsmdroid
import com.example.alertcity.ui.components.configurarOsmdroid

class AlertCityApp : Application() {

    val sessionManager by lazy { SessionManager(applicationContext) }

    val networkMonitor by lazy { NetworkMonitor(applicationContext) }

    val database by lazy { AlertCityDatabase.getDatabase(this) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            api = ApiClient.api,
            sessionManager = sessionManager
        )
    }

    val reporteRepository: ReporteRepository by lazy {
        NetworkBoundReporteRepository(
            context = applicationContext,
            dao = database.reporteDao(),
            api = ApiClient.api,
            sessionManager = sessionManager,
            networkMonitor = networkMonitor
        )
    }

    override fun onCreate() {
        super.onCreate()
        configurarOsmdroid(this)
        NetworkConstants.init(applicationContext)
        ApiClient.init(sessionManager)
        networkMonitor
        SyncScheduler.schedulePeriodicSync(this)
        if (sessionManager.isLoggedIn()) {
            SyncScheduler.enqueueOneTimeSync(this)
        }
    }
}
