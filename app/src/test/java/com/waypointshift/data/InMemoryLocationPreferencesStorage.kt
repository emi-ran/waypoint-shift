package com.waypointshift.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryLocationPreferencesStorage : LocationPreferencesStorage {
    private val _favoritesJson = MutableStateFlow<String?>(null)
    override val favoritesJson: Flow<String?> = _favoritesJson.asStateFlow()

    private val _recentsJson = MutableStateFlow<String?>(null)
    override val recentsJson: Flow<String?> = _recentsJson.asStateFlow()

    private val _onboardingSeen = MutableStateFlow(false)
    override val onboardingSeen: Flow<Boolean> = _onboardingSeen.asStateFlow()

    override suspend fun updateFavorites(transform: (String?) -> String?) {
        _favoritesJson.update(transform)
    }

    override suspend fun updateRecents(transform: (String?) -> String?) {
        _recentsJson.update(transform)
    }

    override suspend fun setOnboardingSeen(seen: Boolean) {
        _onboardingSeen.value = seen
    }
}
