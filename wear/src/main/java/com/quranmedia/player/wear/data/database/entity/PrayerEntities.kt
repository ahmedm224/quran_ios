package com.quranmedia.player.wear.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.wear.domain.model.CalculationMethod
import com.quranmedia.player.wear.domain.model.HijriDate
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.model.UserLocation
import com.quranmedia.player.wear.domain.util.HijriCalendarUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "prayer_times_cache",
    primaryKeys = ["date", "latitude", "longitude", "calculationMethod", "asrMethod"]
)
data class PrayerTimesEntity(
    val date: String,
    val latitude: Double,
    val longitude: Double,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val locationName: String,
    val calculationMethod: Int,
    val asrMethod: Int = 0,
    val hijriDay: Int,
    val hijriMonth: String,
    val hijriMonthArabic: String,
    val hijriYear: Int,
    val hijriMonthNumber: Int = 1,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): PrayerTimes {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return PrayerTimes(
            date = LocalDate.parse(date),
            fajr = LocalTime.parse(fajr, timeFormatter),
            sunrise = LocalTime.parse(sunrise, timeFormatter),
            dhuhr = LocalTime.parse(dhuhr, timeFormatter),
            asr = LocalTime.parse(asr, timeFormatter),
            maghrib = LocalTime.parse(maghrib, timeFormatter),
            isha = LocalTime.parse(isha, timeFormatter),
            locationName = locationName,
            calculationMethod = CalculationMethod.fromId(calculationMethod),
            hijriDate = HijriDate(
                day = hijriDay,
                month = hijriMonth,
                monthArabic = hijriMonthArabic,
                year = hijriYear,
                monthNumber = if (hijriMonthNumber > 0) hijriMonthNumber else HijriCalendarUtils.getMonthNumber(hijriMonth)
            )
        )
    }

    companion object {
        fun fromDomainModel(
            prayerTimes: PrayerTimes,
            latitude: Double,
            longitude: Double,
            asrMethodId: Int = 0
        ): PrayerTimesEntity {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            return PrayerTimesEntity(
                date = prayerTimes.date.toString(),
                latitude = latitude,
                longitude = longitude,
                fajr = prayerTimes.fajr.format(timeFormatter),
                sunrise = prayerTimes.sunrise.format(timeFormatter),
                dhuhr = prayerTimes.dhuhr.format(timeFormatter),
                asr = prayerTimes.asr.format(timeFormatter),
                maghrib = prayerTimes.maghrib.format(timeFormatter),
                isha = prayerTimes.isha.format(timeFormatter),
                locationName = prayerTimes.locationName,
                calculationMethod = prayerTimes.calculationMethod.id,
                asrMethod = asrMethodId,
                hijriDay = prayerTimes.hijriDate.day,
                hijriMonth = prayerTimes.hijriDate.month,
                hijriMonthArabic = prayerTimes.hijriDate.monthArabic,
                hijriYear = prayerTimes.hijriDate.year,
                hijriMonthNumber = prayerTimes.hijriDate.monthNumber
            )
        }
    }
}

@Entity(tableName = "user_location")
data class UserLocationEntity(
    @PrimaryKey
    val id: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val cityName: String? = null,
    val countryName: String? = null,
    val isAutoDetected: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel() = UserLocation(
        latitude = latitude,
        longitude = longitude,
        cityName = cityName,
        countryName = countryName,
        isAutoDetected = isAutoDetected
    )

    companion object {
        fun fromDomainModel(location: UserLocation) = UserLocationEntity(
            id = 1,
            latitude = location.latitude,
            longitude = location.longitude,
            cityName = location.cityName,
            countryName = location.countryName,
            isAutoDetected = location.isAutoDetected
        )
    }
}

@Entity(tableName = "prayer_settings")
data class PrayerSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val calculationMethod: Int = 4, // Default: Umm Al-Qura
    val asrMethod: Int = 0, // Default: Shafi
    val notificationsEnabled: Boolean = true,
    val notifyFajr: Boolean = true,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val minutesBefore: Int = 0,
    val appLanguage: String = "ar" // Default: Arabic
)
