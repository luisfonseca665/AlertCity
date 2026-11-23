package com.example.alertcity.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.alertcity.domain.util.LATITUD_CIUDAD_DEFECTO
import com.example.alertcity.domain.util.LONGITUD_CIUDAD_DEFECTO
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object LocationHelper {

    private const val CURRENT_LOCATION_TIMEOUT_MS = 5_000L

    fun tienePermisoUbicacion(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    /**
     * Obtiene coordenadas GPS precisas mediante FusedLocationProviderClient, con fallback
     * a lastLocation, LocationManager del sistema y coordenadas por defecto de la ciudad.
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerCoordenadas(context: Context): Pair<Double, Double> = withContext(Dispatchers.IO) {
        if (!tienePermisoUbicacion(context)) {
            return@withContext LATITUD_CIUDAD_DEFECTO to LONGITUD_CIUDAD_DEFECTO
        }

        val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val currentLocation = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
            val cancellationTokenSource = CancellationTokenSource()
            awaitTask(
                task = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ),
                onCancel = { cancellationTokenSource.cancel() }
            )
        }
        if (currentLocation != null) {
            return@withContext currentLocation.latitude to currentLocation.longitude
        }

        val lastFusedLocation = withTimeoutOrNull(2_000L) {
            awaitTask(fusedClient.lastLocation)
        }
        if (lastFusedLocation != null) {
            return@withContext lastFusedLocation.latitude to lastFusedLocation.longitude
        }

        val fallbackLocation = obtenerUbicacionFallbackManager(context)
        if (fallbackLocation != null) {
            fallbackLocation.latitude to fallbackLocation.longitude
        } else {
            LATITUD_CIUDAD_DEFECTO to LONGITUD_CIUDAD_DEFECTO
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionFallbackManager(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <T> awaitTask(
        task: Task<T>,
        onCancel: (() -> Unit)? = null
    ): T? = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { onCancel?.invoke() }
        task.addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }.addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }.addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }
}
