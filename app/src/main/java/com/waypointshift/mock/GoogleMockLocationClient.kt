package com.waypointshift.mock

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.annotation.SuppressLint
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.waypointshift.model.GeoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class MockLocationSample(
    val point: GeoPoint,
    val wallTimeMillis: Long,
    val elapsedRealtimeNanos: Long,
    val accuracyMeters: Float = DEFAULT_ACCURACY_METERS,
) {
    companion object {
        const val DEFAULT_ACCURACY_METERS = 3f
    }
}

interface MockLocationGateway {
    fun playServicesAvailability(): Int
    suspend fun setMockMode(enabled: Boolean)
    suspend fun setMockLocation(sample: MockLocationSample)
}

class GoogleMockLocationClient(
    private val gateway: MockLocationGateway,
    private val accessChecker: MockLocationAccessChecker = MockLocationAccessChecker {
        MockLocationAccess.UNKNOWN
    },
) : MockLocationClient {
    constructor(context: Context) : this(
        gateway = AndroidMockLocationGateway(context.applicationContext),
        accessChecker = AndroidMockLocationAccessChecker(context.applicationContext),
    )

    override suspend fun enable(): MockLocationEngine {
        if (accessChecker.check() == MockLocationAccess.NOT_ALLOWED) {
            throw MockLocationClientException(
                kind = MockFailureKind.APP_OP_NOT_ALLOWED,
                operation = MockClientOperation.ENABLE,
                cause = SecurityException("Mock-location AppOp is not allowed"),
            )
        }
        if (gateway.playServicesAvailability() != ConnectionResult.SUCCESS) {
            throw MockLocationClientException(
                kind = MockFailureKind.PLAY_SERVICES_UNAVAILABLE,
                operation = MockClientOperation.ENABLE,
                cause = IllegalStateException("Google Play services unavailable"),
            )
        }
        runOperation(MockClientOperation.ENABLE) { gateway.setMockMode(true) }
        return MockLocationEngine.FUSED
    }

    override suspend fun publish(
        point: GeoPoint,
        wallTimeMillis: Long,
        elapsedRealtimeNanos: Long,
    ) {
        runOperation(MockClientOperation.PUBLISH) {
            gateway.setMockLocation(
                MockLocationSample(
                    point = point,
                    wallTimeMillis = wallTimeMillis,
                    elapsedRealtimeNanos = elapsedRealtimeNanos,
                ),
            )
        }
    }

    override suspend fun disable() {
        runOperation(MockClientOperation.DISABLE) { gateway.setMockMode(false) }
    }

    private suspend fun runOperation(operation: MockClientOperation, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: MockLocationClientException) {
            throw error
        } catch (error: Throwable) {
            throw MockLocationClientException(
                kind = when {
                    operation == MockClientOperation.ENABLE -> MockFailureKind.ENABLE_FAILED
                    operation == MockClientOperation.PUBLISH -> MockFailureKind.PUBLISH_FAILED
                    else -> MockFailureKind.DISABLE_FAILED
                },
                operation = operation,
                cause = error,
            )
        }
    }
}

private class AndroidMockLocationGateway(context: Context) : MockLocationGateway {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val apiAvailability = GoogleApiAvailability.getInstance()
    private val applicationContext = context.applicationContext

    override fun playServicesAvailability(): Int =
        apiAvailability.isGooglePlayServicesAvailable(applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun setMockMode(enabled: Boolean) {
        fusedClient.setMockMode(enabled).awaitResult()
    }

    @SuppressLint("MissingPermission")
    override suspend fun setMockLocation(sample: MockLocationSample) {
        fusedClient.setMockLocation(sample.toAndroidLocation()).awaitResult()
    }

    private fun MockLocationSample.toAndroidLocation(): Location = Location(fusedProvider).apply {
        latitude = point.latitude
        longitude = point.longitude
        accuracy = accuracyMeters
        time = wallTimeMillis
        elapsedRealtimeNanos = this@toAndroidLocation.elapsedRealtimeNanos
    }

    private val fusedProvider: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        } else {
            "fused"
        }
}

private suspend fun Task<Void>.awaitResult(): Unit = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener {
        if (continuation.isActive) continuation.resume(Unit)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
