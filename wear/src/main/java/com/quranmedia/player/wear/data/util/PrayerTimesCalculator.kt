package com.quranmedia.player.wear.data.util

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.CalculationMethod as AdhanCalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.quranmedia.player.wear.domain.model.AsrJuristicMethod
import com.quranmedia.player.wear.domain.model.CalculationMethod
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.util.HijriCalendarUtils
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Offline prayer times calculator using Adhan-Kotlin library.
 * Calculates prayer times using astronomical algorithms - no network required.
 */
object PrayerTimesCalculator {

    /**
     * Calculate prayer times for a given location and date.
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

        val zoneId = ZoneId.systemDefault()

        val fajr = dateToLocalTime(adhanPrayerTimes.fajr, zoneId)
        val sunrise = dateToLocalTime(adhanPrayerTimes.sunrise, zoneId)
        val dhuhr = dateToLocalTime(adhanPrayerTimes.dhuhr, zoneId)
        val asr = dateToLocalTime(adhanPrayerTimes.asr, zoneId)
        val maghrib = dateToLocalTime(adhanPrayerTimes.maghrib, zoneId)
        val isha = dateToLocalTime(adhanPrayerTimes.isha, zoneId)

        val adjustedDate = if (hijriAdjustment != 0) {
            date.plusDays(hijriAdjustment.toLong())
        } else {
            date
        }
        val hijriDate = HijriCalendarUtils.gregorianToHijri(adjustedDate)

        Timber.d("Calculated prayer times for $date: Fajr=$fajr, Dhuhr=$dhuhr, Asr=$asr, Maghrib=$maghrib, Isha=$isha")

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
            CalculationMethod.TEHRAN -> createCustomParameters(17.7, 14.0, 0)
            CalculationMethod.GULF -> createCustomParameters(19.5, 0.0, 90)
            CalculationMethod.FRANCE -> createCustomParameters(12.0, 12.0, 0)
            CalculationMethod.TURKEY -> createCustomParameters(18.0, 17.0, 0)
            CalculationMethod.RUSSIA -> createCustomParameters(16.0, 15.0, 0)
        }

        params.madhab = madhab
        return params
    }

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

    private fun dateToLocalTime(date: Date, zoneId: ZoneId): LocalTime {
        return date.toInstant()
            .atZone(zoneId)
            .toLocalTime()
    }
}
