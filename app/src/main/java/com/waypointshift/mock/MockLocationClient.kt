package com.waypointshift.mock

import com.waypointshift.model.GeoPoint

interface MockLocationClient {
    suspend fun enable()

    suspend fun publish(
        point: GeoPoint,
        wallTimeMillis: Long,
        elapsedRealtimeNanos: Long,
    )

    suspend fun disable()
}

enum class MockClientOperation {
    ENABLE,
    PUBLISH,
    DISABLE,
}

enum class MockFailureKind {
    APP_OP_NOT_ALLOWED,
    PLAY_SERVICES_UNAVAILABLE,
    ENABLE_FAILED,
    PUBLISH_FAILED,
    DISABLE_FAILED,
}

class MockLocationClientException(
    val kind: MockFailureKind,
    val operation: MockClientOperation,
    cause: Throwable,
) : Exception("Mock location ${operation.name.lowercase()} failed", cause)
