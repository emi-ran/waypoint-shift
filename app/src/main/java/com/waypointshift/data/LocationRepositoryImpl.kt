package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocationRepositoryImpl(
    private val storage: LocationPreferencesStorage,
    private val onStorageError: ((Throwable) -> Unit)? = null,
) : LocationRepository {

    override val favorites: Flow<List<SavedLocation>> = storage.favoritesJson.map { raw ->
        LocationDiskDto.decodeList(raw, onStorageError)
    }

    override val recents: Flow<List<SavedLocation>> = storage.recentsJson.map { raw ->
        LocationDiskDto.decodeList(raw, onStorageError)
    }

    override val onboardingSeen: Flow<Boolean> = storage.onboardingSeen

    override suspend fun saveFavorite(
        id: String,
        label: String?,
        point: GeoPoint,
        timestampEpochMillis: Long,
    ): SavedLocation {
        val trimmedLabel = normalizeLabel(label)
        val item = SavedLocation(
            id = id,
            label = trimmedLabel,
            point = point,
            createdAtEpochMillis = timestampEpochMillis,
        )

        storage.updateFavorites { raw ->
            val current = LocationDiskDto.decodeList(raw, onStorageError)
            val index = current.indexOfFirst { it.id == id }
            val updated = if (index >= 0) {
                current.toMutableList().apply { set(index, item) }
            } else {
                current + item
            }
            LocationDiskDto.encodeList(updated)
        }

        return item
    }

    private fun normalizeLabel(label: String?): String? {
        val trimmed = label?.trim()?.takeUnless { it.isEmpty() } ?: return null
        val totalCodePoints = trimmed.codePointCount(0, trimmed.length)
        return if (totalCodePoints > MAX_LABEL_CODE_POINTS) {
            val endOffset = trimmed.offsetByCodePoints(0, MAX_LABEL_CODE_POINTS)
            trimmed.substring(0, endOffset)
        } else {
            trimmed
        }
    }

    override suspend fun deleteFavorite(id: String) {
        storage.updateFavorites { raw ->
            val current = LocationDiskDto.decodeList(raw, onStorageError)
            val updated = current.filterNot { it.id == id }
            LocationDiskDto.encodeList(updated)
        }
    }

    override suspend fun addRecent(point: GeoPoint, timestampEpochMillis: Long): SavedLocation {
        val normalizedPoint = GeoPoint.of(
            latitude = point.formattedLatitude.toDouble(),
            longitude = point.formattedLongitude.toDouble(),
        )
        val item = SavedLocation(
            id = "recent-${normalizedPoint.formattedLatitude}-${normalizedPoint.formattedLongitude}",
            label = null,
            point = normalizedPoint,
            createdAtEpochMillis = timestampEpochMillis,
        )

        storage.updateRecents { raw ->
            val current = LocationDiskDto.decodeList(raw, onStorageError)
            val filtered = current.filterNot {
                it.point.formattedLatitude == normalizedPoint.formattedLatitude &&
                    it.point.formattedLongitude == normalizedPoint.formattedLongitude
            }
            val updated = (listOf(item) + filtered).take(MAX_RECENTS)
            LocationDiskDto.encodeList(updated)
        }

        return item
    }

    override suspend fun clearRecents() {
        storage.updateRecents {
            LocationDiskDto.encodeList(emptyList())
        }
    }

    override suspend fun setOnboardingSeen(seen: Boolean) {
        storage.setOnboardingSeen(seen)
    }

    companion object {
        private const val MAX_LABEL_CODE_POINTS = 60
        private const val MAX_RECENTS = 20
    }
}
