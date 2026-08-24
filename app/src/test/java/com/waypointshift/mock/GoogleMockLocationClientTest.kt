package com.waypointshift.mock

import com.google.android.gms.common.ConnectionResult
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class GoogleMockLocationClientTest {
    @Test
    fun `maps enable publish and disable to gateway in order with exact sample`() = runTest {
        val gateway = RecordingGateway()
        val client = GoogleMockLocationClient(gateway)
        val point = GeoPoint.of(41.0082, 28.9784)

        client.enable()
        client.publish(point, 1234L, 5678L)
        client.disable()

        assertEquals(listOf("mode:true", "publish", "mode:false"), gateway.calls)
        assertEquals(MockLocationSample(point, 1234L, 5678L, 3f), gateway.sample)
    }

    @Test
    fun `maps security cause to setup-required failure retaining cause`() = runTest {
        val cause = SecurityException("not selected")
        val client = GoogleMockLocationClient(RecordingGateway(enableFailure = cause))

        val error = assertThrows(MockLocationClientException::class.java) {
            kotlinx.coroutines.runBlocking { client.enable() }
        }

        assertEquals(MockFailureKind.SETUP_REQUIRED, error.kind)
        assertEquals(MockClientOperation.ENABLE, error.operation)
        assertSame(cause, error.cause)
    }

    @Test
    fun `rejects unavailable Play services before enabling mock mode`() = runTest {
        val gateway = RecordingGateway(availability = ConnectionResult.SERVICE_MISSING)
        val client = GoogleMockLocationClient(gateway)

        val error = assertThrows(MockLocationClientException::class.java) {
            kotlinx.coroutines.runBlocking { client.enable() }
        }

        assertEquals(MockFailureKind.PLAY_SERVICES_UNAVAILABLE, error.kind)
        assertEquals(emptyList<String>(), gateway.calls)
    }

    private class RecordingGateway(
        private val availability: Int = ConnectionResult.SUCCESS,
        private val enableFailure: Throwable? = null,
    ) : MockLocationGateway {
        val calls = mutableListOf<String>()
        var sample: MockLocationSample? = null

        override fun playServicesAvailability(): Int = availability

        override suspend fun setMockMode(enabled: Boolean) {
            if (enabled) enableFailure?.let { throw it }
            calls += "mode:$enabled"
        }

        override suspend fun setMockLocation(sample: MockLocationSample) {
            calls += "publish"
            this.sample = sample
        }
    }
}
