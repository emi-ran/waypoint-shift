package com.waypointshift.mock

import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FallbackMockLocationClientTest {
    @Test
    fun `Fused remains primary when it enables successfully`() = runTest {
        val fused = RecordingClient(MockLocationEngine.FUSED)
        val locationManager = RecordingClient(MockLocationEngine.LOCATION_MANAGER)
        val client = FallbackMockLocationClient(
            fusedClient = fused,
            locationManagerClient = locationManager,
            accessChecker = MockLocationAccessChecker { MockLocationAccess.ALLOWED },
        )

        assertEquals(MockLocationEngine.FUSED, client.enable())
        client.publish(GeoPoint.of(41.0, 29.0), 100L, 200L)
        client.disable()

        assertEquals(listOf("enable", "publish", "disable"), fused.calls)
        assertEquals(emptyList<String>(), locationManager.calls)
    }

    @Test
    fun `allowed AppOp falls back after Fused mock mode enable failure`() = runTest {
        val fused = RecordingClient(
            engine = MockLocationEngine.FUSED,
            enableFailure = clientFailure(MockFailureKind.ENABLE_FAILED),
        )
        val locationManager = RecordingClient(MockLocationEngine.LOCATION_MANAGER)
        val client = FallbackMockLocationClient(
            fusedClient = fused,
            locationManagerClient = locationManager,
            accessChecker = MockLocationAccessChecker { MockLocationAccess.ALLOWED },
        )
        val point = GeoPoint.of(41.0, 29.0)

        assertEquals(MockLocationEngine.LOCATION_MANAGER, client.enable())
        client.publish(point, 100L, 200L)
        client.disable()

        assertEquals(listOf("enable", "disable"), fused.calls)
        assertEquals(listOf("enable", "publish", "disable"), locationManager.calls)
    }

    @Test
    fun `allowed AppOp falls back when Play services cannot provide Fused`() = runTest {
        val fused = RecordingClient(
            engine = MockLocationEngine.FUSED,
            enableFailure = clientFailure(MockFailureKind.PLAY_SERVICES_UNAVAILABLE),
        )
        val locationManager = RecordingClient(MockLocationEngine.LOCATION_MANAGER)
        val client = FallbackMockLocationClient(
            fusedClient = fused,
            locationManagerClient = locationManager,
            accessChecker = MockLocationAccessChecker { MockLocationAccess.ALLOWED },
        )

        assertEquals(MockLocationEngine.LOCATION_MANAGER, client.enable())
        assertEquals(listOf("enable"), locationManager.calls)
    }

    @Test
    fun `unknown AppOp never uses LocationManager fallback`() = runTest {
        val fusedFailure = clientFailure(MockFailureKind.ENABLE_FAILED)
        val locationManager = RecordingClient(MockLocationEngine.LOCATION_MANAGER)
        val client = FallbackMockLocationClient(
            fusedClient = RecordingClient(MockLocationEngine.FUSED, fusedFailure),
            locationManagerClient = locationManager,
            accessChecker = MockLocationAccessChecker { MockLocationAccess.UNKNOWN },
        )

        val error = assertThrows(MockLocationClientException::class.java) {
            kotlinx.coroutines.runBlocking { client.enable() }
        }

        assertEquals(fusedFailure, error)
        assertEquals(emptyList<String>(), locationManager.calls)
    }

    private class RecordingClient(
        private val engine: MockLocationEngine,
        private val enableFailure: MockLocationClientException? = null,
    ) : MockLocationClient {
        val calls = mutableListOf<String>()

        override suspend fun enable(): MockLocationEngine {
            calls += "enable"
            enableFailure?.let { throw it }
            return engine
        }

        override suspend fun publish(point: GeoPoint, wallTimeMillis: Long, elapsedRealtimeNanos: Long) {
            calls += "publish"
        }

        override suspend fun disable() {
            calls += "disable"
        }
    }

    private fun clientFailure(kind: MockFailureKind) = MockLocationClientException(
        kind = kind,
        operation = MockClientOperation.ENABLE,
        cause = IllegalStateException(kind.name),
    )
}
