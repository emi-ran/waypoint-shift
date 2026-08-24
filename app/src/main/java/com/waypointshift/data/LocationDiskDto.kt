package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class LocationDiskDtoV1(
    val id: String,
    val label: String? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAtEpochMillis: Long,
) {
    fun toDomain(): SavedLocation = SavedLocation(
        id = id,
        label = label,
        point = GeoPoint.of(latitude, longitude),
        createdAtEpochMillis = createdAtEpochMillis,
    )

    companion object {
        fun fromDomain(location: SavedLocation): LocationDiskDtoV1 = LocationDiskDtoV1(
            id = location.id,
            label = location.label,
            latitude = location.point.latitude,
            longitude = location.point.longitude,
            createdAtEpochMillis = location.createdAtEpochMillis,
        )
    }
}

object LocationDiskDto {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun encodeList(locations: List<SavedLocation>): String {
        val dtos = locations.map { LocationDiskDtoV1.fromDomain(it) }
        return json.encodeToString(dtos)
    }

    fun decodeList(
        rawJson: String?,
        onError: ((Throwable) -> Unit)? = null,
    ): List<SavedLocation> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            val dtos = json.decodeFromString<List<LocationDiskDtoV1>>(rawJson)
            dtos.mapNotNull { dto ->
                try {
                    dto.toDomain()
                } catch (e: Exception) {
                    onError?.invoke(e)
                    null
                }
            }
        } catch (e: Exception) {
            onError?.invoke(e)
            emptyList()
        }
    }
}
