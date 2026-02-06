package com.quranmedia.player.wear.data.repository

import com.quranmedia.player.wear.data.database.dao.PrayerTimesDao
import com.quranmedia.player.wear.data.database.entity.PrayerSettingsEntity
import com.quranmedia.player.wear.data.database.entity.PrayerTimesEntity
import com.quranmedia.player.wear.data.database.entity.UserLocationEntity
import com.quranmedia.player.wear.data.location.LocationHelper
import com.quranmedia.player.wear.data.util.PrayerTimesCalculator
import com.quranmedia.player.wear.domain.model.AppLanguage
import com.quranmedia.player.wear.domain.model.AsrJuristicMethod
import com.quranmedia.player.wear.domain.model.CalculationMethod
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.model.PrayerType
import com.quranmedia.player.wear.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Repository for prayer times on Wear OS.
 * Uses offline calculation with Adhan library.
 */
@Singleton
class WearPrayerTimesRepository @Inject constructor(
    private val prayerTimesDao: PrayerTimesDao,
    private val locationHelper: LocationHelper
) {
    companion object {
        // Default location: Makkah (used only as fallback when GPS is unavailable)
        val DEFAULT_LOCATION = UserLocation(
            latitude = 21.4225,
            longitude = 39.8262,
            cityName = "Makkah",
            countryName = "Saudi Arabia",
            isAutoDetected = false
        )
    }

    /**
     * Check if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return try {
            locationHelper.hasLocationPermission()
        } catch (e: Exception) {
            Timber.e(e, "Error checking location permission")
            false
        }
    }

    /**
     * Try to detect and save the user's current location via GPS.
     * Returns the detected location or null if detection fails.
     * Never throws - always returns gracefully.
     */
    suspend fun detectAndSaveLocation(): UserLocation? {
        return try {
            val detectedLocation = locationHelper.getCurrentLocation()
            if (detectedLocation != null) {
                saveLocation(detectedLocation)
                Timber.d("Auto-detected location: ${detectedLocation.displayName}")
                detectedLocation
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to detect location")
            null
        }
    }

    /**
     * Get the current location - tries saved location first, then GPS, then default Makkah.
     * Never throws - always returns a valid location.
     */
    suspend fun getCurrentLocation(): UserLocation {
        return try {
            // First check if we have a saved location
            val savedLocation = prayerTimesDao.getSavedLocationSync()?.toDomainModel()
            if (savedLocation != null) {
                return savedLocation
            }

            // Try GPS if no saved location
            val gpsLocation = locationHelper.getCurrentLocation()
            if (gpsLocation != null) {
                saveLocation(gpsLocation)
                return gpsLocation
            }

            // Fall back to Makkah
            DEFAULT_LOCATION
        } catch (e: Exception) {
            Timber.e(e, "Error getting location, using default")
            DEFAULT_LOCATION
        }
    }

    /**
     * Get prayer times for today with cached results or fresh calculation.
     * Uses GPS location if available, otherwise falls back to saved/default location.
     */
    suspend fun getPrayerTimesForToday(): PrayerTimes {
        val settings = getSettingsSync()
        val location = getCurrentLocation()
        val date = LocalDate.now()
        val method = CalculationMethod.fromId(settings.calculationMethod)
        val asrMethod = AsrJuristicMethod.fromId(settings.asrMethod)

        Timber.d("Getting prayer times for location: ${location.displayName} (${location.latitude}, ${location.longitude})")

        // Check cache first
        val cached = prayerTimesDao.getCachedPrayerTimes(
            date = date.toString(),
            latitude = location.latitude,
            longitude = location.longitude,
            calculationMethod = method.id,
            asrMethod = asrMethod.id
        )

        if (cached != null) {
            Timber.d("Using cached prayer times for $date")
            return cached.toDomainModel()
        }

        // Calculate fresh
        val prayerTimes = PrayerTimesCalculator.calculate(
            latitude = location.latitude,
            longitude = location.longitude,
            date = date,
            method = method,
            asrMethod = asrMethod,
            locationName = location.displayName
        )

        // Cache the result
        val entity = PrayerTimesEntity.fromDomainModel(prayerTimes, location.latitude, location.longitude, asrMethod.id)
        prayerTimesDao.cachePrayerTimes(entity)

        Timber.d("Calculated and cached prayer times for $date")
        return prayerTimes
    }

    /**
     * Get prayer times for a specific date.
     */
    suspend fun getPrayerTimes(date: LocalDate): PrayerTimes {
        val settings = getSettingsSync()
        val location = getCurrentLocation()
        val method = CalculationMethod.fromId(settings.calculationMethod)
        val asrMethod = AsrJuristicMethod.fromId(settings.asrMethod)

        // Check cache
        val cached = prayerTimesDao.getCachedPrayerTimes(
            date = date.toString(),
            latitude = location.latitude,
            longitude = location.longitude,
            calculationMethod = method.id,
            asrMethod = asrMethod.id
        )

        if (cached != null) {
            return cached.toDomainModel()
        }

        // Calculate
        val prayerTimes = PrayerTimesCalculator.calculate(
            latitude = location.latitude,
            longitude = location.longitude,
            date = date,
            method = method,
            asrMethod = asrMethod,
            locationName = location.displayName
        )

        // Cache
        val entity = PrayerTimesEntity.fromDomainModel(prayerTimes, location.latitude, location.longitude, asrMethod.id)
        prayerTimesDao.cachePrayerTimes(entity)

        return prayerTimes
    }

    /**
     * Get the next prayer and time remaining.
     * After Isha, returns tomorrow's Fajr.
     */
    suspend fun getNextPrayer(): Pair<PrayerType, LocalTime>? {
        val prayerTimes = getPrayerTimesForToday()
        val now = LocalTime.now()

        val prayerList = listOf(
            PrayerType.FAJR to prayerTimes.fajr,
            PrayerType.SUNRISE to prayerTimes.sunrise,
            PrayerType.DHUHR to prayerTimes.dhuhr,
            PrayerType.ASR to prayerTimes.asr,
            PrayerType.MAGHRIB to prayerTimes.maghrib,
            PrayerType.ISHA to prayerTimes.isha
        )

        // Find the next prayer
        for ((type, time) in prayerList) {
            if (time.isAfter(now)) {
                return type to time
            }
        }

        // After Isha, next is tomorrow's Fajr
        val tomorrowPrayerTimes = getPrayerTimes(LocalDate.now().plusDays(1))
        return PrayerType.FAJR to tomorrowPrayerTimes.fajr
    }

    /**
     * Flow for observing prayer times for a date.
     */
    fun observePrayerTimes(date: LocalDate): Flow<PrayerTimes?> {
        return prayerTimesDao.getPrayerTimesForDate(date.toString()).map { entity ->
            entity?.toDomainModel()
        }
    }

    // Location operations

    suspend fun saveLocation(location: UserLocation) {
        val entity = UserLocationEntity.fromDomainModel(location)
        prayerTimesDao.saveLocation(entity)
        // Clear cache when location changes
        prayerTimesDao.clearAllCache()
        Timber.d("Saved location: ${location.displayName}")
    }

    fun getSavedLocation(): Flow<UserLocation?> {
        return prayerTimesDao.getSavedLocation().map { entity ->
            entity?.toDomainModel() ?: DEFAULT_LOCATION
        }
    }

    suspend fun getSavedLocationSync(): UserLocation? {
        return prayerTimesDao.getSavedLocationSync()?.toDomainModel() ?: DEFAULT_LOCATION
    }

    // Settings operations

    fun observeSettings(): Flow<PrayerSettingsEntity?> {
        return prayerTimesDao.getSettings()
    }

    suspend fun getSettingsSync(): PrayerSettingsEntity {
        return prayerTimesDao.getSettingsSync() ?: PrayerSettingsEntity()
    }

    suspend fun saveSettings(settings: PrayerSettingsEntity) {
        prayerTimesDao.saveSettings(settings)
        // Clear cache when settings change
        prayerTimesDao.clearAllCache()
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        val current = getSettingsSync()
        saveSettings(current.copy(calculationMethod = method.id))
    }

    suspend fun updateAsrMethod(method: AsrJuristicMethod) {
        val current = getSettingsSync()
        saveSettings(current.copy(asrMethod = method.id))
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = getSettingsSync()
        saveSettings(current.copy(notificationsEnabled = enabled))
    }

    suspend fun setPrayerNotificationEnabled(prayerType: PrayerType, enabled: Boolean) {
        val current = getSettingsSync()
        val updated = when (prayerType) {
            PrayerType.FAJR -> current.copy(notifyFajr = enabled)
            PrayerType.DHUHR -> current.copy(notifyDhuhr = enabled)
            PrayerType.ASR -> current.copy(notifyAsr = enabled)
            PrayerType.MAGHRIB -> current.copy(notifyMaghrib = enabled)
            PrayerType.ISHA -> current.copy(notifyIsha = enabled)
            else -> current
        }
        saveSettings(updated)
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        val current = getSettingsSync()
        saveSettings(current.copy(appLanguage = language.code))
    }

    suspend fun getAppLanguage(): AppLanguage {
        val settings = getSettingsSync()
        return AppLanguage.fromCode(settings.appLanguage)
    }

    /**
     * Check if notification is enabled for a specific prayer.
     */
    suspend fun isNotificationEnabledForPrayer(prayerType: PrayerType): Boolean {
        val settings = getSettingsSync()
        if (!settings.notificationsEnabled) return false

        return when (prayerType) {
            PrayerType.FAJR -> settings.notifyFajr
            PrayerType.DHUHR -> settings.notifyDhuhr
            PrayerType.ASR -> settings.notifyAsr
            PrayerType.MAGHRIB -> settings.notifyMaghrib
            PrayerType.ISHA -> settings.notifyIsha
            else -> false
        }
    }

    suspend fun clearOldCache(daysToKeep: Int = 7) {
        val threshold = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        prayerTimesDao.deleteOldCache(threshold)
    }
}
