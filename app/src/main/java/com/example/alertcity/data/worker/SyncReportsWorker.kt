package com.example.alertcity.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alertcity.AlertCityApp
import com.example.alertcity.data.remote.ApiClient
import com.example.alertcity.data.remote.NetworkConstants

class SyncReportsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AlertCityApp ?: return Result.failure()

        // Garantizar inicialización de red y autenticación en ejecución en background
        NetworkConstants.init(applicationContext)
        ApiClient.init(app.sessionManager)

        if (!app.sessionManager.isLoggedIn()) {
            Log.d(TAG, "Sin sesión iniciada; se pospone la sincronización.")
            return Result.success()
        }

        val syncResult = app.reporteRepository.syncPendingReports()
        return syncResult.fold(
            onSuccess = { count ->
                Log.d(TAG, "Sincronización en background completada ($count reportes).")
                Result.success()
            },
            onFailure = { error ->
                Log.w(TAG, "Error transitorio en SyncReportsWorker; se reintentará.", error)
                Result.retry()
            }
        )
    }

    companion object {
        private const val TAG = "SyncReportsWorker"
    }
}
