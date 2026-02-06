package com.quranmedia.player.data.repository

import com.quranmedia.player.data.database.dao.PrayerTimesDao
import com.quranmedia.player.data.database.entity.PrayerTimesEntity
import com.quranmedia.player.data.database.entity.UserLocationEntity
import com.quranmedia.player.data.util.PrayerTimesCalculator
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.UserLocation
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prayer times repository implementation using offline calculation.
 * Uses Adhan-Kotlin library for astronomical calculations - no network required.
 */
@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    private val prayerTimesDao: PrayerTimesDao,
    private val settingsRepository: SettingsRepository
) : PrayerTimesRepository {

    companion object {
        // Default fallback location: Makkah, Saudi Arabia
        private const val DEFAULT_LATITUDE = 21.4225
        private const val DEFAULT_LONGITUDE = 39.8262
        private const val DEFAULT_CITY_NAME = "Makkah"
        private const val DEFAULT_COUNTRY = "Saudi Arabia"

        val DEFAULT_LOCATION = UserLocation(
            latitude = DEFAULT_LATITUDE,
            longitude = DEFAULT_LONGITUDE,
            cityName = DEFAULT_CITY_NAME,
            countryName = DEFAULT_COUNTRY,
            isAutoDetected = false  // Fallback location, not auto-detected
        )
    }

    override suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ): Resource<PrayerTimes> {
        // 1. Check cache first for quick UI access
        try {
            val cached = prayerTimesDao.getCachedPrayerTimes(
                date = date.toString(),
                latitude = latitude,
                longitude = longitude,
                calculationMethod = method.id,
                asrMethod = asrMethod.id
            )
            if (cached != null) {
                Timber.d("Using cached prayer times for $date")
                return Resource.Success(cached.toDomainModel())
            }
        } catch (e: Exception) {
            Timber.w(e, "Cache lookup failed, will calculate")
        }

        // 2. Calculate offline using Adhan-Kotlin (instant, no network needed)
        return try {
            val locationName = getLocationName(latitude, longitude)

            val prayerTimes = PrayerTimesCalculator.calculate(
                latitude = latitude,
                longitude = longitude,
                date = date,
                method = method,
                asrMethod = asrMethod,
                locationName = locationName
            )

            // Cache the result for quick future access
            val entity = PrayerTimesEntity.fromDomainModel(prayerTimes, latitude, longitude, asrMethod.id)
            prayerTimesDao.cachePrayerTimes(entity)

            Timber.d("Calculated and cached prayer times for $date")
            Resource.Success(prayerTimes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate prayer times")
            Resource.Error("Failed to calculate prayer times: ${e.message}")
        }
    }

    /**
     * Get a display name for the location.
     * First tries to find a saved location, otherwise returns coordinates.
     */
    private suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            val savedLocation = prayerTimesDao.getSavedLocationSync()
            if (savedLocation != null &&
                isNearby(savedLocation.latitude, savedLocation.longitude, latitude, longitude)) {
                // Use saved location name if coordinates are nearby
                buildString {
                    savedLocation.cityName?.let { append(it) }
                    savedLocation.countryName?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                }.ifEmpty { formatCoordinates(latitude, longitude) }
            } else {
                formatCoordinates(latitude, longitude)
            }
        } catch (e: Exception) {
            formatCoordinates(latitude, longitude)
        }
    }

    /**
     * Check if two coordinates are nearby (within ~1km)
     */
    private fun isNearby(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        val threshold = 0.01 // ~1km at equator
        return kotlin.math.abs(lat1 - lat2) < threshold &&
               kotlin.math.abs(lon1 - lon2) < threshold
    }

    /**
     * Format coordinates as a readable string
     */
    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "%.4f, %.4f".format(latitude, longitude)
    }

    override fun getCachedPrayerTimes(date: LocalDate): Flow<PrayerTimes?> {
        return prayerTimesDao.getPrayerTimesForDate(date.toString()).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun saveLocation(location: UserLocation) {
        val entity = UserLocationEntity.fromDomainModel(location)
        prayerTimesDao.saveLocation(entity)
        Timber.d("Saved location: ${location.cityName}")
    }

    override fun getSavedLocation(): Flow<UserLocation?> {
        return prayerTimesDao.getSavedLocation().map { entity ->
            entity?.toDomainModel() ?: DEFAULT_LOCATION
        }
    }

    override suspend fun getSavedLocationSync(): UserLocation? {
        return prayerTimesDao.getSavedLocationSync()?.toDomainModel() ?: DEFAULT_LOCATION
    }

    override suspend fun getSavedCalculationMethod(): CalculationMethod {
        val methodId = settingsRepository.getCurrentSettings().prayerCalculationMethod
        return CalculationMethod.fromId(methodId)
    }

    override suspend fun saveCalculationMethod(method: CalculationMethod) {
        settingsRepository.setPrayerCalculationMethod(method.id)
    }

    override suspend fun clearOldCache(daysToKeep: Int) {
        val threshold = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        prayerTimesDao.deleteOldCache(threshold)
        Timber.d("Cleared prayer times cache older than $daysToKeep days")
    }
}
