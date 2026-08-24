package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LocationRepositoryImpl(
    private val storage: LocationPreferencesStorage,
    scope: CoroutineScope,
    private val onStorageError: ((Throwable) -> Unit)? = null,
) : LocationRepository {

    override val favorites: StateFlow<List<SavedLocation>> = storage.favoritesJson
        .map { raw -> LocationDiskDto.decodeList(raw, onStorageError) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    override val recents: StateFlow<List<SavedLocation>> = storage.recentsJson
        .map { raw -> LocationDiskDto.decodeList(raw, onStorageError) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    override val onboardingSeen: StateFlow<Boolean> = storage.onboardingSeen
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    override suspend fun saveFavorite(
        id: String,
        label: String?,
        point: GeoPoint,
        timestampEpochMillis: Long,
    ): SavedLocation {
        require(id.isNotBlank()) { "Favorite ID cannot be blank" }

        val trimmedLabel = normalizeLabel(label)
        var resultItem: SavedLocation? = null

        storage.updateFavorites { raw ->
            val current = LocationDiskDto.decodeList(raw, onStorageError)
            val index = current.indexOfFirst { it.id == id }
            val item = if (index >= 0) {
                SavedLocation(
                    id = id,
                    label = trimmedLabel,
                    point = point,
                    createdAtEpochMillis = current[index].createdAtEpochMillis,
                    lastUsedAtEpochMillis = timestampEpochMillis,
                )
            } else {
                SavedLocation(
                    id = id,
                    label = trimmedLabel,
                    point = point,
                    createdAtEpochMillis = timestampEpochMillis,
                    lastUsedAtEpochMillis = timestampEpochMillis,
                )
            }
            resultItem = item
            val updated = if (index >= 0) {
                current.toMutableList().apply { set(index, item) }
            } else {
                current + item
            }
            LocationDiskDto.encodeList(updated)
        }

        val savedItem = checkNotNull(resultItem)
        favorites.first { items -> items.any { it == savedItem } }
        return savedItem
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
        require(id.isNotBlank()) { "Favorite ID cannot be blank" }
        var updatedIds: Set<String> = emptySet()
        storage.updateFavorites { raw ->
            val current = LocationDiskDto.decodeList(raw, onStorageError)
            val updated = current.filterNot { it.id == id }
            updatedIds = updated.mapTo(mutableSetOf()) { it.id }
            LocationDiskDto.encodeList(updated)
        }
        favorites.first { items -> items.mapTo(mutableSetOf()) { it.id } == updatedIds }
    }

    override suspend fun addRecent(point: GeoPoint, timestampEpochMillis: Long): SavedLocation {
        val normalizedPoint = GeoPoint.of(
            latitude = point.formattedLatitude.toDouble(),
            longitude = point.formattedLongitude.toDouble(),
        )

        var resultItem: SavedLocation? = null

        storage.updateRecents { raw ->
            val current = LocationDiskDto.decodeList(raw, onStorageError)
            val existing = current.firstOrNull {
                it.point.formattedLatitude == normalizedPoint.formattedLatitude &&
                    it.point.formattedLongitude == normalizedPoint.formattedLongitude
            }

            val item = SavedLocation(
                id = existing?.id ?: "recent-${normalizedPoint.formattedLatitude}-${normalizedPoint.formattedLongitude}",
                label = null,
                point = normalizedPoint,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: timestampEpochMillis,
                lastUsedAtEpochMillis = timestampEpochMillis,
            )
            resultItem = item

            val filtered = current.filterNot {
                it.point.formattedLatitude == normalizedPoint.formattedLatitude &&
                    it.point.formattedLongitude == normalizedPoint.formattedLongitude
            }
            val updated = (listOf(item) + filtered).take(MAX_RECENTS)
            LocationDiskDto.encodeList(updated)
        }

        val recentItem = checkNotNull(resultItem)
        recents.first { items -> items.firstOrNull() == recentItem }
        return recentItem
    }

    override suspend fun clearRecents() {
        storage.updateRecents {
            LocationDiskDto.encodeList(emptyList())
        }
        recents.first { it.isEmpty() }
    }

    override suspend fun setOnboardingSeen(seen: Boolean) {
        storage.setOnboardingSeen(seen)
        onboardingSeen.first { it == seen }
    }

    companion object {
        private const val MAX_LABEL_CODE_POINTS = 60
        private const val MAX_RECENTS = 20
    }
}
