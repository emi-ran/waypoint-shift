package com.waypointshift.mock

import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import com.waypointshift.model.GeoPoint

internal enum class TestProviderApi {
    LEGACY,
    PROVIDER_PROPERTIES,
}

internal fun testProviderApiFor(sdkInt: Int): TestProviderApi =
    if (sdkInt >= Build.VERSION_CODES.S) {
        TestProviderApi.PROVIDER_PROPERTIES
    } else {
        TestProviderApi.LEGACY
    }

internal data class TestProviderSpec(
    val name: String,
    val requiresNetwork: Boolean,
    val requiresSatellite: Boolean,
    val powerUsage: Int,
    val accuracy: Int,
)

internal interface LocationManagerMockGateway {
    fun addTestProvider(spec: TestProviderSpec)
    fun setTestProviderEnabled(provider: String, enabled: Boolean)
    fun setTestProviderLocation(provider: String, sample: MockLocationSample)
    fun removeTestProvider(provider: String)
}

internal class LocationManagerMockLocationClient(
    private val gateway: LocationManagerMockGateway,
) : MockLocationClient {
    constructor(locationManager: LocationManager) : this(AndroidLocationManagerMockGateway(locationManager))

    private val ownedProviders = mutableListOf<String>()

    override suspend fun enable(): MockLocationEngine {
        if (ownedProviders.isNotEmpty()) return MockLocationEngine.LOCATION_MANAGER
        try {
            PROVIDERS.forEach { spec ->
                gateway.addTestProvider(spec)
                ownedProviders += spec.name
                gateway.setTestProviderEnabled(spec.name, true)
            }
            return MockLocationEngine.LOCATION_MANAGER
        } catch (error: Throwable) {
            cleanupOwnedProviders()
            throw MockLocationClientException(
                kind = MockFailureKind.ENABLE_FAILED,
                operation = MockClientOperation.ENABLE,
                cause = error,
            )
        }
    }

    override suspend fun publish(
        point: GeoPoint,
        wallTimeMillis: Long,
        elapsedRealtimeNanos: Long,
    ) {
        val sample = MockLocationSample(
            point = point,
            wallTimeMillis = wallTimeMillis.coerceAtLeast(MIN_VALID_TIMESTAMP),
            elapsedRealtimeNanos = elapsedRealtimeNanos.coerceAtLeast(MIN_VALID_TIMESTAMP),
        )
        try {
            ownedProviders.forEach { provider ->
                gateway.setTestProviderLocation(provider, sample)
            }
        } catch (error: Throwable) {
            throw MockLocationClientException(
                kind = MockFailureKind.PUBLISH_FAILED,
                operation = MockClientOperation.PUBLISH,
                cause = error,
            )
        }
    }

    override suspend fun disable() {
        val cleanupError = cleanupOwnedProviders()
        if (cleanupError != null) {
            throw MockLocationClientException(
                kind = MockFailureKind.DISABLE_FAILED,
                operation = MockClientOperation.DISABLE,
                cause = cleanupError,
            )
        }
    }

    private fun cleanupOwnedProviders(): Throwable? {
        var firstError: Throwable? = null
        ownedProviders.asReversed().forEach { provider ->
            runCatching { gateway.setTestProviderEnabled(provider, false) }
                .onFailure { if (firstError == null) firstError = it }
            runCatching { gateway.removeTestProvider(provider) }
                .onFailure { if (firstError == null) firstError = it }
        }
        ownedProviders.clear()
        return firstError
    }

    private companion object {
        const val MIN_VALID_TIMESTAMP = 1L
        val PROVIDERS = listOf(
            TestProviderSpec(
                name = LocationManager.GPS_PROVIDER,
                requiresNetwork = false,
                requiresSatellite = true,
                powerUsage = ProviderProperties.POWER_USAGE_HIGH,
                accuracy = ProviderProperties.ACCURACY_FINE,
            ),
            TestProviderSpec(
                name = LocationManager.NETWORK_PROVIDER,
                requiresNetwork = true,
                requiresSatellite = false,
                powerUsage = ProviderProperties.POWER_USAGE_LOW,
                accuracy = ProviderProperties.ACCURACY_COARSE,
            ),
        )
    }
}

private class AndroidLocationManagerMockGateway(
    private val locationManager: LocationManager,
) : LocationManagerMockGateway {
    override fun addTestProvider(spec: TestProviderSpec) {
        if (testProviderApiFor(Build.VERSION.SDK_INT) == TestProviderApi.PROVIDER_PROPERTIES) {
            locationManager.addTestProvider(
                spec.name,
                ProviderProperties.Builder()
                    .setHasNetworkRequirement(spec.requiresNetwork)
                    .setHasSatelliteRequirement(spec.requiresSatellite)
                    .setPowerUsage(spec.powerUsage)
                    .setAccuracy(spec.accuracy)
                    .build(),
            )
        } else {
            @Suppress("DEPRECATION")
            locationManager.addTestProvider(
                spec.name,
                spec.requiresNetwork,
                spec.requiresSatellite,
                false,
                false,
                false,
                false,
                false,
                spec.powerUsage,
                spec.accuracy,
            )
        }
    }

    override fun setTestProviderEnabled(provider: String, enabled: Boolean) {
        locationManager.setTestProviderEnabled(provider, enabled)
    }

    override fun setTestProviderLocation(provider: String, sample: MockLocationSample) {
        locationManager.setTestProviderLocation(
            provider,
            Location(provider).apply {
                latitude = sample.point.latitude
                longitude = sample.point.longitude
                accuracy = sample.accuracyMeters
                time = sample.wallTimeMillis
                elapsedRealtimeNanos = sample.elapsedRealtimeNanos
            },
        )
    }

    override fun removeTestProvider(provider: String) {
        locationManager.removeTestProvider(provider)
    }
}
