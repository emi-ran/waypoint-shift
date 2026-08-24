package com.waypointshift.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoPointTest {
    @Test
    fun `accepts coordinate boundaries`() {
        assertEquals(-90.0, GeoPoint.of(-90.0, -180.0).latitude, 0.0)
        assertEquals(-180.0, GeoPoint.of(-90.0, -180.0).longitude, 0.0)
        assertEquals(90.0, GeoPoint.of(90.0, 180.0).latitude, 0.0)
        assertEquals(180.0, GeoPoint.of(90.0, 180.0).longitude, 0.0)
    }

    @Test
    fun `rejects latitude outside supported range`() {
        assertInvalid(
            expectedCoordinate = CoordinateComponent.LATITUDE,
            expectedReason = CoordinateErrorReason.OUT_OF_RANGE,
        ) { GeoPoint.of(-90.000001, 0.0) }
        assertInvalid(
            expectedCoordinate = CoordinateComponent.LATITUDE,
            expectedReason = CoordinateErrorReason.OUT_OF_RANGE,
        ) { GeoPoint.of(90.000001, 0.0) }
    }

    @Test
    fun `rejects longitude outside supported range`() {
        assertInvalid(
            expectedCoordinate = CoordinateComponent.LONGITUDE,
            expectedReason = CoordinateErrorReason.OUT_OF_RANGE,
        ) { GeoPoint.of(0.0, -180.000001) }
        assertInvalid(
            expectedCoordinate = CoordinateComponent.LONGITUDE,
            expectedReason = CoordinateErrorReason.OUT_OF_RANGE,
        ) { GeoPoint.of(0.0, 180.000001) }
    }

    @Test
    fun `rejects non-finite coordinates`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            assertInvalid(
                expectedCoordinate = CoordinateComponent.LATITUDE,
                expectedReason = CoordinateErrorReason.NOT_FINITE,
            ) { GeoPoint.of(value, 0.0) }
            assertInvalid(
                expectedCoordinate = CoordinateComponent.LONGITUDE,
                expectedReason = CoordinateErrorReason.NOT_FINITE,
            ) { GeoPoint.of(0.0, value) }
        }
    }

    @Test
    fun `parses signed dot-decimal coordinates with surrounding whitespace`() {
        val point = GeoPoint.parse("  +41.015137  ", " -28.979530 ")

        assertEquals(41.015137, point.latitude, 0.0)
        assertEquals(-28.979530, point.longitude, 0.0)
    }

    @Test
    fun `parses coordinates with trailing decimal points`() {
        val point = GeoPoint.parse("+41.", "-28.")

        assertEquals(41.0, point.latitude, 0.0)
        assertEquals(-28.0, point.longitude, 0.0)
    }

    @Test
    fun `formats coordinates canonically with six decimal places`() {
        val point = GeoPoint.of(-0.0, 28.9)

        assertEquals("0.000000", point.formattedLatitude)
        assertEquals("28.900000", point.formattedLongitude)
    }

    @Test
    fun `rejects malformed coordinate text`() {
        listOf("", " ", "41,5", "1e2", "NaN", "Infinity", "+", "--1", "1.2.3").forEach { value ->
            assertInvalid(
                expectedCoordinate = CoordinateComponent.LATITUDE,
                expectedReason = CoordinateErrorReason.INVALID_FORMAT,
            ) { GeoPoint.parse(value, "0") }
        }
    }

    @Test
    fun `reports component and range reason for parsed out-of-range values`() {
        assertInvalid(
            expectedCoordinate = CoordinateComponent.LONGITUDE,
            expectedReason = CoordinateErrorReason.OUT_OF_RANGE,
        ) { GeoPoint.parse("0", "180.1") }
    }

    private fun assertInvalid(
        expectedCoordinate: CoordinateComponent,
        expectedReason: CoordinateErrorReason,
        block: () -> Unit,
    ) {
        val error = assertThrows(InvalidCoordinateException::class.java, block)
        assertEquals(expectedCoordinate, error.component)
        assertEquals(expectedReason, error.reason)
    }
}
