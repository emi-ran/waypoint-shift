package com.waypointshift.mock

import com.waypointshift.model.GeoPoint

internal class FallbackMockLocationClient(
    private val fusedClient: MockLocationClient,
    private val locationManagerClient: MockLocationClient,
    private val accessChecker: MockLocationAccessChecker,
) : MockLocationClient {
    private var activeClient: MockLocationClient? = null

    override suspend fun enable(): MockLocationEngine {
        activeClient?.let { return it.enable() }
        try {
            return fusedClient.enable().also { activeClient = fusedClient }
        } catch (fusedError: MockLocationClientException) {
            if (!fusedError.allowsLocationManagerFallback() ||
                accessChecker.check() != MockLocationAccess.ALLOWED
            ) {
                throw fusedError
            }
            try {
                fusedClient.disable()
            } catch (cleanupError: Throwable) {
                throw MockLocationClientException(
                    kind = MockFailureKind.DISABLE_FAILED,
                    operation = MockClientOperation.DISABLE,
                    cause = cleanupError,
                )
            }
            return locationManagerClient.enable().also { activeClient = locationManagerClient }
        }
    }

    override suspend fun publish(
        point: GeoPoint,
        wallTimeMillis: Long,
        elapsedRealtimeNanos: Long,
    ) {
        checkNotNull(activeClient) { "Mock location client is not enabled" }
            .publish(point, wallTimeMillis, elapsedRealtimeNanos)
    }

    override suspend fun disable() {
        val client = activeClient ?: return
        try {
            client.disable()
        } finally {
            activeClient = null
        }
    }

    private fun MockLocationClientException.allowsLocationManagerFallback(): Boolean =
        operation == MockClientOperation.ENABLE &&
            (kind == MockFailureKind.ENABLE_FAILED || kind == MockFailureKind.PLAY_SERVICES_UNAVAILABLE)
}
