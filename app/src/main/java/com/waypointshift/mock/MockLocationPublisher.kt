package com.waypointshift.mock

import android.os.SystemClock
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

interface MockClock {
    fun currentTimeMillis(): Long
    fun elapsedRealtimeNanos(): Long
}

object AndroidMockClock : MockClock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeNanos(): Long = SystemClock.elapsedRealtimeNanos()
}

sealed interface MockPublisherStatus {
    data object Idle : MockPublisherStatus
    data class Starting(val point: GeoPoint) : MockPublisherStatus
    data class Active(val point: GeoPoint) : MockPublisherStatus
    data class Failed(val failure: MockSessionFailure) : MockPublisherStatus
}

data class MockSessionFailure(
    val kind: MockFailureKind,
    val operation: MockClientOperation,
    val cleanupFailed: Boolean,
    val cause: Throwable,
)

interface MockPublisher {
    val status: StateFlow<MockPublisherStatus>

    suspend fun start(point: GeoPoint)
    suspend fun stop()
}

class MockLocationPublisher(
    private val client: MockLocationClient,
    private val scope: CoroutineScope,
    private val clock: MockClock = AndroidMockClock,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val cleanupTimeoutMillis: Long = DEFAULT_CLEANUP_TIMEOUT_MILLIS,
) : MockPublisher {
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow<MockPublisherStatus>(MockPublisherStatus.Idle)
    private var publicationJob: Job? = null
    private var currentPoint: GeoPoint? = null
    private var cleanupRequired = false

    override val status: StateFlow<MockPublisherStatus> = mutableStatus.asStateFlow()

    init {
        require(intervalMillis > 0) { "Publication interval must be positive" }
        require(cleanupTimeoutMillis > 0) { "Cleanup timeout must be positive" }
    }

    override suspend fun start(point: GeoPoint) {
        mutex.withLock {
            if (publicationJob?.isActive == true) {
                try {
                    publish(point)
                    currentPoint = point
                    mutableStatus.value = MockPublisherStatus.Active(point)
                } catch (error: Throwable) {
                    publicationJob?.cancelAndJoin()
                    failAndCleanup(error, operationFor(error, MockClientOperation.PUBLISH))
                }
                return
            }

            mutableStatus.value = MockPublisherStatus.Starting(point)
            cleanupRequired = true
            try {
                client.enable()
                publish(point)
                currentPoint = point
                mutableStatus.value = MockPublisherStatus.Active(point)
                publicationJob = scope.launch { publicationLoop() }
            } catch (cancellation: CancellationException) {
                cleanupAfterCancellation()
                throw cancellation
            } catch (error: Throwable) {
                failAndCleanup(error, operationFor(error, MockClientOperation.ENABLE))
            }
        }
    }

    override suspend fun stop() {
        mutex.withLock {
            publicationJob?.cancelAndJoin()
            publicationJob = null
            currentPoint = null

            if (!cleanupRequired) {
                if (mutableStatus.value !is MockPublisherStatus.Failed) {
                    mutableStatus.value = MockPublisherStatus.Idle
                }
                return
            }

            val cleanupError = disableSafely()
            cleanupRequired = false
            mutableStatus.value = if (cleanupError == null) {
                MockPublisherStatus.Idle
            } else {
                MockPublisherStatus.Failed(
                    MockSessionFailure(
                        kind = MockFailureKind.DISABLE_FAILED,
                        operation = MockClientOperation.DISABLE,
                        cleanupFailed = true,
                        cause = cleanupError,
                    ),
                )
            }
        }
    }

    private suspend fun publicationLoop() {
        var nextPublishNanos = clock.elapsedRealtimeNanos() + intervalMillis * NANOS_PER_MILLISECOND
        while (scope.isActive) {
            val remainingNanos = nextPublishNanos - clock.elapsedRealtimeNanos()
            if (remainingNanos > 0) {
                delay((remainingNanos + NANOS_PER_MILLISECOND - 1) / NANOS_PER_MILLISECOND)
            }

            var failure: Throwable? = null
            mutex.withLock {
                val point = currentPoint ?: return
                try {
                    publish(point)
                } catch (error: Throwable) {
                    failure = error
                }
            }

            failure?.let { error ->
                mutex.withLock {
                    publicationJob = null
                    failAndCleanup(error, operationFor(error, MockClientOperation.PUBLISH))
                }
                return
            }
            nextPublishNanos += intervalMillis * NANOS_PER_MILLISECOND
        }
    }

    private suspend fun publish(point: GeoPoint) {
        client.publish(
            point = point,
            wallTimeMillis = clock.currentTimeMillis(),
            elapsedRealtimeNanos = clock.elapsedRealtimeNanos(),
        )
    }

    private suspend fun failAndCleanup(error: Throwable, operation: MockClientOperation) {
        currentPoint = null
        publicationJob = null
        val cleanupError = disableSafely()
        cleanupRequired = false
        val clientError = error as? MockLocationClientException
        mutableStatus.value = MockPublisherStatus.Failed(
            MockSessionFailure(
                kind = clientError?.kind ?: operation.defaultFailureKind(),
                operation = clientError?.operation ?: operation,
                cleanupFailed = cleanupError != null,
                cause = error,
            ),
        )
    }

    private suspend fun cleanupAfterCancellation() {
        currentPoint = null
        publicationJob = null
        disableSafely()
        cleanupRequired = false
        mutableStatus.value = MockPublisherStatus.Idle
    }

    private suspend fun disableSafely(): Throwable? = withContext(NonCancellable) {
        runCatching {
            withTimeout(cleanupTimeoutMillis) {
                client.disable()
            }
        }.exceptionOrNull()
    }

    private fun operationFor(error: Throwable, fallback: MockClientOperation): MockClientOperation =
        (error as? MockLocationClientException)?.operation ?: fallback

    private fun MockClientOperation.defaultFailureKind(): MockFailureKind = when (this) {
        MockClientOperation.ENABLE -> MockFailureKind.ENABLE_FAILED
        MockClientOperation.PUBLISH -> MockFailureKind.PUBLISH_FAILED
        MockClientOperation.DISABLE -> MockFailureKind.DISABLE_FAILED
    }

    private companion object {
        const val DEFAULT_INTERVAL_MILLIS = 1_000L
        const val DEFAULT_CLEANUP_TIMEOUT_MILLIS = 5_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
