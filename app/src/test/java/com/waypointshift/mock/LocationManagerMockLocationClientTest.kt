package com.waypointshift.mock

import android.location.LocationManager
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationManagerMockLocationClientTest {
    @Test
    fun `enable manages gps and network providers with API-aware specs`() = runTest {
        val gateway = RecordingLocationManagerGateway()
        val client = LocationManagerMockLocationClient(gateway)

        assertEquals(MockLocationEngine.LOCATION_MANAGER, client.enable())

        assertEquals(listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER), gateway.added.map { it.name })
        assertTrue(gateway.added.first { it.name == LocationManager.GPS_PROVIDER }.requiresSatellite)
        assertTrue(gateway.added.first { it.name == LocationManager.NETWORK_PROVIDER }.requiresNetwork)
        assertEquals(listOf("gps:true", "network:true"), gateway.enabled)
        assertEquals(TestProviderApi.LEGACY, testProviderApiFor(30))
        assertEquals(TestProviderApi.PROVIDER_PROPERTIES, testProviderApiFor(31))
    }

    @Test
    fun `publish sends safe complete timestamps to both providers`() = runTest {
        val gateway = RecordingLocationManagerGateway()
        val client = LocationManagerMockLocationClient(gateway)
        val point = GeoPoint.of(41.0082, 28.9784)
        client.enable()

        client.publish(point, wallTimeMillis = -10L, elapsedRealtimeNanos = 0L)

        assertEquals(listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER), gateway.published.map { it.first })
        gateway.published.forEach { (_, sample) ->
            assertEquals(point, sample.point)
            assertEquals(1L, sample.wallTimeMillis)
            assertEquals(1L, sample.elapsedRealtimeNanos)
        }
    }

    @Test
    fun `partial enable failure removes every provider already owned`() = runTest {
        val gateway = RecordingLocationManagerGateway(failEnableProvider = LocationManager.NETWORK_PROVIDER)
        val client = LocationManagerMockLocationClient(gateway)

        runCatching { client.enable() }

        assertEquals(listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER), gateway.removed)
    }

    @Test
    fun `disable turns off and removes both providers in reverse order`() = runTest {
        val gateway = RecordingLocationManagerGateway()
        val client = LocationManagerMockLocationClient(gateway)
        client.enable()

        client.disable()
        client.disable()

        assertEquals(
            listOf("network:false", "gps:false"),
            gateway.enabled.takeLast(2),
        )
        assertEquals(listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER), gateway.removed)
    }

    private class RecordingLocationManagerGateway(
        private val failEnableProvider: String? = null,
    ) : LocationManagerMockGateway {
        val added = mutableListOf<TestProviderSpec>()
        val enabled = mutableListOf<String>()
        val published = mutableListOf<Pair<String, MockLocationSample>>()
        val removed = mutableListOf<String>()

        override fun addTestProvider(spec: TestProviderSpec) {
            added += spec
        }

        override fun setTestProviderEnabled(provider: String, enabled: Boolean) {
            this.enabled += "$provider:$enabled"
            if (enabled && provider == failEnableProvider) error("enable failed")
        }

        override fun setTestProviderLocation(provider: String, sample: MockLocationSample) {
            published += provider to sample
        }

        override fun removeTestProvider(provider: String) {
            removed += provider
        }
    }
}
