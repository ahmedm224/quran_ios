package com.quranmedia.player.wear.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.quranmedia.player.wear.domain.model.UserLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Helper for getting the user's current location via GPS.
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the current location.
     * Returns null if location cannot be obtained.
     */
    suspend fun getCurrentLocation(): UserLocation? {
        if (!hasLocationPermission()) {
            Timber.w("Location permission not granted")
            return null
        }

        return try {
            val location = getLastLocation() ?: getCurrentLocationFromProvider()
            location?.let { loc ->
                val (cityName, countryName) = getCityAndCountry(loc.latitude, loc.longitude)
                UserLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    cityName = cityName,
                    countryName = countryName,
                    isAutoDetected = true
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get location")
            null
        }
    }

    /**
     * Get the last known location (faster than getting current).
     */
    @Suppress("MissingPermission")
    private suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                cont.resume(location)
            }
            .addOnFailureListener { e: Exception ->
                Timber.e(e, "Failed to get last location")
                cont.resume(null)
            }
    }

    /**
     * Request current location from provider.
     */
    @Suppress("MissingPermission")
    private suspend fun getCurrentLocationFromProvider(): Location? = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            cont.resume(location)
        }.addOnFailureListener { e: Exception ->
            Timber.e(e, "Failed to get current location")
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }

    /**
     * Get city and country name from coordinates using Geocoder.
     */
    private fun getCityAndCountry(latitude: Double, longitude: Double): Pair<String?, String?> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea
                val country = address.countryName
                Pair(city, country)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Geocoding failed")
            Pair(null, null)
        }
    }
}
