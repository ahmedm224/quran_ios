package com.quranmedia.player.data.repository

import com.quranmedia.player.data.api.AladhanApi
import com.quranmedia.player.data.database.dao.PrayerTimesDao
import com.quranmedia.player.data.database.entity.PrayerTimesEntity
import com.quranmedia.player.data.database.entity.UserLocationEntity
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.HijriDate
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.UserLocation
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    private val aladhanApi: AladhanApi,
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

    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    override suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ): Resource<PrayerTimes> {
        return try {
            // Fetch from API (don't cache to ensure Asr method is always current)
            val dateString = date.format(dateFormatter)
            val response = aladhanApi.getPrayerTimes(
                date = dateString,
                latitude = latitude,
                longitude = longitude,
                method = method.id,
                school = asrMethod.id
            )

            if (response.code != 200) {
                return Resource.Error("API error: ${response.status}")
            }

            val data = response.data
            val timings = data.timings
            val hijri = data.date.hijri

            // Parse times (format: "HH:mm (timezone)")
            val prayerTimes = PrayerTimes(
                date = date,
                fajr = parseTime(timings.fajr),
                sunrise = parseTime(timings.sunrise),
                dhuhr = parseTime(timings.dhuhr),
                asr = parseTime(timings.asr),
                maghrib = parseTime(timings.maghrib),
                isha = parseTime(timings.isha),
                locationName = "${data.meta.timezone}",
                calculationMethod = method,
                hijriDate = HijriDate(
                    day = hijri.day.toIntOrNull() ?: 1,
                    month = hijri.month.en,
                    monthArabic = hijri.month.ar,
                    year = hijri.year.toIntOrNull() ?: 1446,
                    monthNumber = hijri.month.number
                )
            )

            // Cache the result
            val entity = PrayerTimesEntity.fromDomainModel(prayerTimes, latitude, longitude)
            prayerTimesDao.cachePrayerTimes(entity)

            Timber.d("Fetched and cached prayer times for $date")
            Resource.Success(prayerTimes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get prayer times")
            Resource.Error("Failed to get prayer times: ${e.message}")
        }
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

    /**
     * Parse time string from API (format: "HH:mm" or "HH:mm (TZ)")
     */
    private fun parseTime(timeString: String): LocalTime {
        // Remove timezone suffix if present (e.g., "05:30 (AST)" -> "05:30")
        val cleanTime = timeString.substringBefore(" ").substringBefore("(").trim()
        return try {
            LocalTime.parse(cleanTime, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            Timber.w("Failed to parse time: $timeString, defaulting to midnight")
            LocalTime.MIDNIGHT
        }
    }
}
