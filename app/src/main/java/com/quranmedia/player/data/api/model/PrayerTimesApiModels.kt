package com.quranmedia.player.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Aladhan API response models
 */
data class PrayerTimesApiResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: PrayerTimesData
)

data class PrayerTimesData(
    @SerializedName("timings") val timings: Timings,
    @SerializedName("date") val date: DateInfo,
    @SerializedName("meta") val meta: MetaInfo
)

data class Timings(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Sunrise") val sunrise: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Sunset") val sunset: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String,
    @SerializedName("Imsak") val imsak: String,
    @SerializedName("Midnight") val midnight: String,
    @SerializedName("Firstthird") val firstthird: String?,
    @SerializedName("Lastthird") val lastthird: String?
)

data class DateInfo(
    @SerializedName("readable") val readable: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("gregorian") val gregorian: GregorianInfo,
    @SerializedName("hijri") val hijri: HijriInfo
)

data class GregorianInfo(
    @SerializedName("date") val date: String,
    @SerializedName("format") val format: String,
    @SerializedName("day") val day: String,
    @SerializedName("weekday") val weekday: WeekdayInfo,
    @SerializedName("month") val month: MonthInfo,
    @SerializedName("year") val year: String
)

data class HijriInfo(
    @SerializedName("date") val date: String,
    @SerializedName("format") val format: String,
    @SerializedName("day") val day: String,
    @SerializedName("weekday") val weekday: WeekdayInfo,
    @SerializedName("month") val month: HijriMonthInfo,
    @SerializedName("year") val year: String,
    @SerializedName("designation") val designation: DesignationInfo,
    @SerializedName("holidays") val holidays: List<String>?
)

data class WeekdayInfo(
    @SerializedName("en") val en: String,
    @SerializedName("ar") val ar: String?
)

data class MonthInfo(
    @SerializedName("number") val number: Int,
    @SerializedName("en") val en: String
)

data class HijriMonthInfo(
    @SerializedName("number") val number: Int,
    @SerializedName("en") val en: String,
    @SerializedName("ar") val ar: String
)

data class DesignationInfo(
    @SerializedName("abbreviated") val abbreviated: String,
    @SerializedName("expanded") val expanded: String
)

data class MetaInfo(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("method") val method: MethodInfo,
    @SerializedName("latitudeAdjustmentMethod") val latitudeAdjustmentMethod: String,
    @SerializedName("midnightMode") val midnightMode: String,
    @SerializedName("school") val school: String,
    @SerializedName("offset") val offset: Map<String, Int>
)

data class MethodInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("params") val params: MethodParams
)

data class MethodParams(
    @SerializedName("Fajr") val fajr: Double?,
    @SerializedName("Isha") val isha: Any? // Can be Double or String like "90 min"
)
