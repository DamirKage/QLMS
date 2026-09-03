package kz.qlms.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

object LocationUtils {

    /**
     * Single high-accuracy fix. Callers are expected to have already checked
     * the location permission (Compose screens do this via Accompanist before
     * calling in; the SOS service is only ever started after that check too).
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(10_000)
            .build()
        return runCatching { client.getCurrentLocation(request, null).await() }.getOrNull()
            ?: runCatching { client.lastLocation.await() }.getOrNull()
    }

    fun googleMapsLink(latitude: Double, longitude: Double): String =
        "https://maps.google.com/?q=$latitude,$longitude"
}
