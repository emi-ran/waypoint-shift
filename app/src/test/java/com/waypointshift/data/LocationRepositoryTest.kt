package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationRepositoryTest {
    @Test
    fun `exposes hot StateFlow snapshots with initial synchronous values`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(
            storage = storage,
            scope = backgroundScope,
        )

        assertEquals(emptyList<SavedLocation>(), repository.favorites.value)
        assertEquals(emptyList<SavedLocation>(), repository.recents.value)
        assertEquals(false, repository.onboardingSeen.value)
    }

    @Test
    fun `updates StateFlow snapshots after repository mutations`() = runTest {
        val repository = LocationRepositoryImpl(
            storage = InMemoryLocationPreferencesStorage(),
            scope = backgroundScope,
        )
        val point = GeoPoint.of(41.0082, 28.9784)

        val favorite = repository.saveFavorite("fav-1", "Ayasofya", point, 1000L)
        val recent = repository.addRecent(point, 2000L)
        repository.setOnboardingSeen(true)

        assertEquals(listOf(favorite), repository.favorites.value)
        assertEquals(listOf(recent), repository.recents.value)
        assertEquals(true, repository.onboardingSeen.value)
    }

    @Test
    fun `adds renames and deletes favorite location preserving creation timestamp`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(
            storage = storage,
            scope = backgroundScope,
        )

        val point = GeoPoint.of(41.0082, 28.9784)
        val added = repository.saveFavorite(
            id = "fav-1",
            label = "Hagia Sophia",
            point = point,
            timestampEpochMillis = 1000L,
        )

        assertEquals("fav-1", added.id)
        assertEquals("Hagia Sophia", added.label)
        assertEquals(1000L, added.createdAtEpochMillis)
        assertEquals(1000L, added.lastUsedAtEpochMillis)
        assertEquals(listOf(added), repository.favorites.first { it.isNotEmpty() })

        val renamed = repository.saveFavorite(
            id = "fav-1",
            label = "Ayasofya",
            point = point,
            timestampEpochMillis = 2000L,
        )

        assertEquals("Ayasofya", renamed.label)
        assertEquals(1000L, renamed.createdAtEpochMillis)
        assertEquals(2000L, renamed.lastUsedAtEpochMillis)
        assertEquals(listOf(renamed), repository.favorites.first { it.firstOrNull()?.label == "Ayasofya" })

        repository.deleteFavorite("fav-1")
        assertEquals(emptyList<SavedLocation>(), repository.favorites.first { it.isEmpty() })
    }

    @Test
    fun `rejects blank stable ID for favorites`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(
            storage = storage,
            scope = backgroundScope,
        )

        val point = GeoPoint.of(41.0082, 28.9784)
        val error = runCatching {
            repository.saveFavorite(
                id = "   ",
                label = "Test",
                point = point,
                timestampEpochMillis = 1000L,
            )
        }.exceptionOrNull()

        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
    }

    @Test
    fun `favorites allow duplicate coordinates under distinct IDs and trim labels to 60 code points`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(
            storage = storage,
            scope = backgroundScope,
        )

        val point = GeoPoint.of(41.0082, 28.9784)
        val longLabel = "📍" + "A".repeat(65)

        val fav1 = repository.saveFavorite(
            id = "fav-1",
            label = "  First Marker  ",
            point = point,
            timestampEpochMillis = 1000L,
        )
        val fav2 = repository.saveFavorite(
            id = "fav-2",
            label = longLabel,
            point = point,
            timestampEpochMillis = 2000L,
        )

        val favorites = repository.favorites.first { it.size == 2 }
        assertEquals(2, favorites.size)
        assertEquals("First Marker", fav1.label)
        assertEquals(60, fav2.label?.codePointCount(0, fav2.label!!.length))
        assertEquals(listOf(fav1, fav2), favorites)
    }

    @Test
    fun `recents normalize to six decimals, deduplicate, move newest to front, and cap at 20`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(
            storage = storage,
            scope = backgroundScope,
        )

        for (i in 1..25) {
            repository.addRecent(
                point = GeoPoint.of(40.0 + (i * 0.001), 29.0),
                timestampEpochMillis = 1000L + i,
            )
        }

        var recents = repository.recents.first { it.size == 20 }
        assertEquals(20, recents.size)
        assertEquals(GeoPoint.of(40.025, 29.0), recents.first().point)
        assertEquals(1025L, recents.first().lastUsedAtEpochMillis)

        val reAdded = repository.addRecent(
            point = GeoPoint.of(40.0250000001, 29.0000000002),
            timestampEpochMillis = 5000L,
        )

        recents = repository.recents.first { it.firstOrNull()?.lastUsedAtEpochMillis == 5000L }
        assertEquals(20, recents.size)
        assertEquals(reAdded, recents.first())
        assertEquals(1025L, recents.first().createdAtEpochMillis)
        assertEquals(5000L, recents.first().lastUsedAtEpochMillis)
        assertEquals(GeoPoint.of(40.025, 29.0), recents.first().point)
        assertEquals(1, recents.count { it.point.formattedLatitude == "40.025000" && it.point.formattedLongitude == "29.000000" })

        repository.clearRecents()
        assertEquals(emptyList<SavedLocation>(), repository.recents.first { it.isEmpty() })
    }
}
