package com.waypointshift.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPreferencesDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `persists favorites and recents across recreated repository instances using real DataStore`() = runTest {
        val testFile = File(tempFolder.root, "test_preferences.preferences_pb")
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dataStoreScope1 = CoroutineScope(testDispatcher + Job())

        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = dataStoreScope1,
            produceFile = { testFile },
        )
        val storage1 = PreferencesLocationPreferencesStorage(dataStore1)
        val repository1 = LocationRepositoryImpl(storage1)

        val point = GeoPoint.of(41.015137, 28.979530)
        repository1.saveFavorite("fav-1", "Sultanahmet", point, 1000L)
        repository1.addRecent(point, 1000L)
        repository1.setOnboardingSeen(true)

        dataStoreScope1.cancel()

        // Recreate repository and storage on second instance pointing to same file
        val dataStoreScope2 = CoroutineScope(testDispatcher + Job())
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = dataStoreScope2,
            produceFile = { testFile },
        )
        val storage2 = PreferencesLocationPreferencesStorage(dataStore2)
        val repository2 = LocationRepositoryImpl(storage2)

        val favorites = repository2.favorites.first()
        val recents = repository2.recents.first()
        val onboardingSeen = repository2.onboardingSeen.first()

        assertEquals(1, favorites.size)
        assertEquals("fav-1", favorites.first().id)
        assertEquals("Sultanahmet", favorites.first().label)
        assertEquals(point, favorites.first().point)

        assertEquals(1, recents.size)
        assertEquals(point, recents.first().point)

        assertTrue(onboardingSeen)

        dataStoreScope2.cancel()
    }

    @Test
    fun `recovers gracefully from corrupted json in Preferences DataStore and invokes error callback`() = runTest {
        val testFile = File(tempFolder.root, "test_corrupt_preferences.preferences_pb")
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dataStoreScope = CoroutineScope(testDispatcher + Job())

        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile },
        )

        // Write corrupt JSON directly into Preferences
        dataStore.edit { preferences ->
            preferences[PreferencesLocationPreferencesStorage.FAVORITES_JSON_KEY] = "{ broken JSON: true "
        }

        var errorReported: Throwable? = null
        val storage = PreferencesLocationPreferencesStorage(dataStore)
        val repository = LocationRepositoryImpl(
            storage = storage,
            onStorageError = { errorReported = it },
        )

        val favorites = repository.favorites.first()
        assertEquals(emptyList<com.waypointshift.model.SavedLocation>(), favorites)
        assertNotNull(errorReported)

        dataStoreScope.cancel()
    }
}
