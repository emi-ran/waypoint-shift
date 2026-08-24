package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRepositoryTest {
    @Test
    fun `adds renames and deletes favorite location`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(storage)

        val point = GeoPoint.of(41.0082, 28.9784)
        val added = repository.saveFavorite(
            id = "fav-1",
            label = "Hagia Sophia",
            point = point,
            timestampEpochMillis = 1000L,
        )

        assertEquals("fav-1", added.id)
        assertEquals("Hagia Sophia", added.label)
        assertEquals(listOf(added), repository.favorites.first())

        val renamed = repository.saveFavorite(
            id = "fav-1",
            label = "Ayasofya",
            point = point,
            timestampEpochMillis = 2000L,
        )
        assertEquals("Ayasofya", renamed.label)
        assertEquals(listOf(renamed), repository.favorites.first())

        repository.deleteFavorite("fav-1")
        assertTrue(repository.favorites.first().isEmpty())
    }

    @Test
    fun `favorites allow duplicate coordinates under distinct IDs and trim labels to 60 code points`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(storage)

        val point = GeoPoint.of(41.0082, 28.9784)
        // 70 emojis/characters to test 60 unicode code points cap
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

        val favorites = repository.favorites.first()
        assertEquals(2, favorites.size)
        assertEquals("First Marker", fav1.label)
        assertEquals(60, fav2.label?.codePointCount(0, fav2.label!!.length))
        assertEquals(listOf(fav1, fav2), favorites)
    }

    @Test
    fun `recents normalize to six decimals, deduplicate, move newest to front, and cap at 20`() = runTest {
        val storage = InMemoryLocationPreferencesStorage()
        val repository = LocationRepositoryImpl(storage)

        // Add 25 distinct locations
        for (i in 1..25) {
            repository.addRecent(
                point = GeoPoint.of(40.0 + (i * 0.001), 29.0),
                timestampEpochMillis = 1000L + i,
            )
        }

        var recents = repository.recents.first()
        assertEquals(20, recents.size)
        // Newest should be index 25
        assertEquals(GeoPoint.of(40.025, 29.0), recents.first().point)

        // Add coordinate identical when normalized to 6 decimals (e.g. 40.0250000001 vs 40.025)
        val reAdded = repository.addRecent(
            point = GeoPoint.of(40.0250000001, 29.0000000002),
            timestampEpochMillis = 5000L,
        )

        recents = repository.recents.first()
        assertEquals(20, recents.size)
        assertEquals(reAdded, recents.first())
        assertEquals(5000L, recents.first().createdAtEpochMillis)
        assertEquals(GeoPoint.of(40.025, 29.0), recents.first().point)
        // Count entries matching this normalized coordinate should be 1
        assertEquals(1, recents.count { it.point.formattedLatitude == "40.025000" && it.point.formattedLongitude == "29.000000" })

        repository.clearRecents()
        assertTrue(repository.recents.first().isEmpty())
    }
}
