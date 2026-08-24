package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import kotlinx.coroutines.flow.Flow

interface LocationPreferencesStorage {
    val favoritesJson: Flow<String?>
    val recentsJson: Flow<String?>
    val onboardingSeen: Flow<Boolean>

    suspend fun updateFavorites(transform: (String?) -> String?)
    suspend fun updateRecents(transform: (String?) -> String?)
    suspend fun setOnboardingSeen(seen: Boolean)
}

interface LocationRepository {
    val favorites: Flow<List<SavedLocation>>
    val recents: Flow<List<SavedLocation>>
    val onboardingSeen: Flow<Boolean>

    suspend fun saveFavorite(
        id: String,
        label: String?,
        point: GeoPoint,
        timestampEpochMillis: Long,
    ): SavedLocation

    suspend fun deleteFavorite(id: String)
    suspend fun addRecent(point: GeoPoint, timestampEpochMillis: Long): SavedLocation
    suspend fun clearRecents()
    suspend fun setOnboardingSeen(seen: Boolean)
}
