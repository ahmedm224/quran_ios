package com.quranmedia.player.data.util

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.CalculationMethod as AdhanCalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.HijriDate
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.util.HijriCalendarUtils
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Offline prayer times calculator using Adhan-Java library.
 * Calculates prayer times using astronomical algorithms - no network required.
 */
object PrayerTimesCalculator {

    /**
     * Calculate prayer times for a given location and date.
     *
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param date Gregorian date to calculate for
     * @param method Calculation method (e.g., MAKKAH, MWL, ISNA)
     * @param asrMethod Asr juristic method (SHAFI or HANAFI)
     * @param locationName Display name for the location
     * @param hijriAdjustment Days to adjust Hijri date (-2 to +2) for regional moon sighting
     * @return PrayerTimes domain model
     */
    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod,
        locationName: String,
        hijriAdjustment: Int = 0
    ): PrayerTimes {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val params = getCalculationParameters(method, asrMethod)

        val adhanPrayerTimes = AdhanPrayerTimes(coordinates, dateComponents, params)

        // Convert java.util.Date to LocalTime using system timezone
        val zoneId = ZoneId.systemDefault()

        val fajr = dateToLocalTime(adhanPrayerTimes.fajr, zoneId)
        val sunrise = dateToLocalTime(adhanPrayerTimes.sunrise, zoneId)
        val dhuhr = dateToLocalTime(adhanPrayerTimes.dhuhr, zoneId)
        val asr = dateToLocalTime(adhanPrayerTimes.asr, zoneId)
        val maghrib = dateToLocalTime(adhanPrayerTimes.maghrib, zoneId)
        val isha = dateToLocalTime(adhanPrayerTimes.isha, zoneId)

        // Calculate Hijri date using existing utility
        val adjustedDate = if (hijriAdjustment != 0) {
            date.plusDays(hijriAdjustment.toLong())
        } else {
            date
        }
        val hijriDate = HijriCalendarUtils.gregorianToHijri(adjustedDate)

        Timber.d("Calculated prayer times for $date at ($latitude, $longitude): Fajr=$fajr, Dhuhr=$dhuhr, Asr=$asr, Maghrib=$maghrib, Isha=$isha")

        return PrayerTimes(
            date = date,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            locationName = locationName,
            calculationMethod = method,
            hijriDate = hijriDate
        )
    }

    /**
     * Map app CalculationMethod to Adhan CalculationParameters
     */
    private fun getCalculationParameters(
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ): CalculationParameters {
        val madhab = when (asrMethod) {
            AsrJuristicMethod.SHAFI -> Madhab.SHAFI
            AsrJuristicMethod.HANAFI -> Madhab.HANAFI
        }

        val params = when (method) {
            CalculationMethod.MAKKAH -> AdhanCalculationMethod.UMM_AL_QURA.parameters
            CalculationMethod.MWL -> AdhanCalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            CalculationMethod.EGYPT -> AdhanCalculationMethod.EGYPTIAN.parameters
            CalculationMethod.KARACHI -> AdhanCalculationMethod.KARACHI.parameters
            CalculationMethod.ISNA -> AdhanCalculationMethod.NORTH_AMERICA.parameters
            CalculationMethod.KUWAIT -> AdhanCalculationMethod.KUWAIT.parameters
            CalculationMethod.QATAR -> AdhanCalculationMethod.QATAR.parameters
            CalculationMethod.DUBAI -> AdhanCalculationMethod.DUBAI.parameters
            CalculationMethod.SINGAPORE -> AdhanCalculationMethod.SINGAPORE.parameters

            // Custom methods not directly supported by Adhan - use custom angles
            CalculationMethod.TEHRAN -> createCustomParameters(
                fajrAngle = 17.7,
                ishaAngle = 14.0,
                ishaInterval = 0
            )
            CalculationMethod.GULF -> createCustomParameters(
                fajrAngle = 19.5,
                ishaAngle = 0.0,
                ishaInterval = 90 // 90 minutes after Maghrib
            )
            CalculationMethod.FRANCE -> createCustomParameters(
                fajrAngle = 12.0,
                ishaAngle = 12.0,
                ishaInterval = 0
            )
            CalculationMethod.TURKEY -> createCustomParameters(
                fajrAngle = 18.0,
                ishaAngle = 17.0,
                ishaInterval = 0
            )
            CalculationMethod.RUSSIA -> createCustomParameters(
                fajrAngle = 16.0,
                ishaAngle = 15.0,
                ishaInterval = 0
            )
        }

        // Set the madhab (Asr calculation method)
        params.madhab = madhab
        return params
    }

    /**
     * Create custom calculation parameters for methods not built into Adhan
     */
    private fun createCustomParameters(
        fajrAngle: Double,
        ishaAngle: Double,
        ishaInterval: Int
    ): CalculationParameters {
        val params = AdhanCalculationMethod.OTHER.parameters
        params.fajrAngle = fajrAngle
        if (ishaInterval > 0) {
            params.ishaInterval = ishaInterval
        } else {
            params.ishaAngle = ishaAngle
        }
        return params
    }

    /**
     * Convert java.util.Date (in UTC) to java.time.LocalTime in specified timezone
     */
    private fun dateToLocalTime(date: Date, zoneId: ZoneId): LocalTime {
        return date.toInstant()
            .atZone(zoneId)
            .toLocalTime()
    }
}
