package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.HijriDate
import com.quranmedia.player.domain.model.PrayerTimes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "prayer_times_cache",
    primaryKeys = ["date", "latitude", "longitude", "calculationMethod", "asrMethod"]
)
data class PrayerTimesEntity(
    val date: String, // ISO date format YYYY-MM-DD
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
    val asrMethod: Int = 0, // 0 = Shafi, 1 = Hanafi
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
                monthNumber = if (hijriMonthNumber > 0) hijriMonthNumber else com.quranmedia.player.domain.util.HijriCalendarUtils.getMonthNumber(hijriMonth)
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
