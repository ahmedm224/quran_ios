package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.UserLocation

@Entity(tableName = "user_location")
data class UserLocationEntity(
    @PrimaryKey
    val id: Int = 1, // Single row table
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
