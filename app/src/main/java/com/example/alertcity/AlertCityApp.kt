package com.example.alertcity

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.alertcity.data.local.SessionManager
import com.example.alertcity.data.local.database.AlertCityDatabase
import com.example.alertcity.data.remote.ApiClient
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.data.remote.NetworkMonitor
import com.example.alertcity.data.repository.AuthRepository
import com.example.alertcity.data.repository.NetworkBoundReporteRepository
import com.example.alertcity.data.repository.RoomUsuarioRepository
import com.example.alertcity.data.worker.SyncScheduler
import com.example.alertcity.domain.repository.ReporteRepository
import com.example.alertcity.domain.repository.UsuarioRepository
import com.example.alertcity.ui.components.configurarOsmdroid
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class AlertCityApp : Application(), ImageLoaderFactory {

    val sessionManager by lazy { SessionManager(applicationContext) }

    val networkMonitor by lazy { NetworkMonitor(applicationContext) }

    val database by lazy { AlertCityDatabase.getDatabase(this) }

    val usuarioRepository: UsuarioRepository by lazy {
        RoomUsuarioRepository(database.usuarioDao())
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            api = ApiClient.api,
            sessionManager = sessionManager,
            usuarioRepository = usuarioRepository
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

    override fun newImageLoader(): ImageLoader {
        val imageHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val token = sessionManager.getAccessToken()
                val esHostBackend = request.url.host.equals(NetworkConstants.hostIp, ignoreCase = true)
                val finalRequest = if (!token.isNullOrBlank() && esHostBackend && request.header("Authorization") == null) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
                chain.proceed(finalRequest)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageHttpClient)
            .crossfade(true)
            .build()
    }
}
