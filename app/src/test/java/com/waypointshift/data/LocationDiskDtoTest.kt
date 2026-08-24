package com.waypointshift.data

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationDiskDtoTest {
    @Test
    fun `serializes and deserializes saved location round trip`() {
        val original = SavedLocation(
            id = "fav-1",
            label = "Galata Tower",
            point = GeoPoint.of(41.025658, 28.974155),
            createdAtEpochMillis = 1724457600000L,
        )

        val json = LocationDiskDto.encodeList(listOf(original))
        val restored = LocationDiskDto.decodeList(json)

        assertEquals(listOf(original), restored)
    }

    @Test
    fun `deserializes and ignores unknown fields safely`() {
        val rawJson = """
            [
              {
                "id": "fav-unknown",
                "label": "Custom Point",
                "latitude": 41.0,
                "longitude": 29.0,
                "createdAtEpochMillis": 1724457600000,
                "futureField": "some_value",
                "extraNested": { "v": 1 }
              }
            ]
        """.trimIndent()

        val restored = LocationDiskDto.decodeList(rawJson)

        assertEquals(1, restored.size)
        assertEquals("fav-unknown", restored[0].id)
        assertEquals("Custom Point", restored[0].label)
        assertEquals(41.0, restored[0].point.latitude, 0.0)
        assertEquals(29.0, restored[0].point.longitude, 0.0)
        assertEquals(1724457600000L, restored[0].createdAtEpochMillis)
    }

    @Test
    fun `recovers from malformed JSON to empty list without crashing and records issue`() {
        var loggedError: Throwable? = null
        val malformedJson = "{ broken json: true "

        val restored = LocationDiskDto.decodeList(malformedJson, onError = { loggedError = it })

        assertEquals(emptyList<SavedLocation>(), restored)
        org.junit.Assert.assertNotNull(loggedError)
    }
}
