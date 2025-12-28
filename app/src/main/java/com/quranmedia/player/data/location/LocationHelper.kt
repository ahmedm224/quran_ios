package com.quranmedia.player.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.UserLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if app has location permission
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
     * Get current location using GPS
     */
    suspend fun getCurrentLocation(): Resource<UserLocation> {
        if (!hasLocationPermission()) {
            return Resource.Error("Location permission not granted")
        }

        return try {
            val location = getLastKnownLocation()
            if (location != null) {
                val userLocation = locationToUserLocation(location)
                Resource.Success(userLocation)
            } else {
                // Try to get fresh location
                val freshLocation = requestFreshLocation()
                if (freshLocation != null) {
                    val userLocation = locationToUserLocation(freshLocation)
                    Resource.Success(userLocation)
                } else {
                    Resource.Error("Could not get location")
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception getting location")
            Resource.Error("Location permission denied")
        } catch (e: Exception) {
            Timber.e(e, "Error getting location")
            Resource.Error("Error getting location: ${e.message}")
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to get last location")
                    continuation.resume(null)
                }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener { e ->
                Timber.e(e, "Failed to get current location")
                continuation.resume(null)
            }
        }
    }

    /**
     * Convert Android Location to UserLocation with geocoded city name
     */
    private fun locationToUserLocation(location: Location): UserLocation {
        var cityName: String? = null
        var countryName: String? = null

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
                countryName = addresses[0].countryName
            }
        } catch (e: Exception) {
            Timber.e(e, "Geocoding failed")
        }

        return UserLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            cityName = cityName,
            countryName = countryName,
            isAutoDetected = true
        )
    }

    /**
     * Create UserLocation from manual city input
     */
    fun createManualLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        countryName: String? = null
    ): UserLocation {
        return UserLocation(
            latitude = latitude,
            longitude = longitude,
            cityName = cityName,
            countryName = countryName,
            isAutoDetected = false
        )
    }

    /**
     * Get coordinates from city name using geocoding
     */
    suspend fun getLocationFromCityName(cityName: String): Resource<UserLocation> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(cityName, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                Resource.Success(
                    UserLocation(
                        latitude = address.latitude,
                        longitude = address.longitude,
                        cityName = address.locality ?: cityName,
                        countryName = address.countryName,
                        isAutoDetected = false
                    )
                )
            } else {
                Resource.Error("City not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Geocoding failed for city: $cityName")
            Resource.Error("Could not find location for: $cityName")
        }
    }
}
