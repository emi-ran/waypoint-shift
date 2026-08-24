package com.waypointshift.service

import com.waypointshift.mock.MockPublisher
import com.waypointshift.mock.MockPublisherStatus
import com.waypointshift.mock.MockSessionFailure
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MockServiceLifecycle(
    private val publisher: MockPublisher,
    private val statusStore: MockServiceStatusStore,
    private val scope: CoroutineScope,
    private val callbacks: Callbacks,
) {
    private val commandMutex = Mutex()
    private var cleanupJob: Job? = null
    private var cleanedUp = false
    private var latestStartId = 0
    private var lastPoint: GeoPoint? = null
    private var terminalFailure: MockSessionFailure? = null

    init {
        scope.launch {
            publisher.status.collectLatest(::handlePublisherStatus)
        }
    }

    fun handle(command: MockServiceCommand?, startId: Int) {
        latestStartId = maxOf(latestStartId, startId)
        when (command) {
            is MockServiceCommand.Start -> start(command.point)
            is MockServiceCommand.Update -> start(command.point)
            MockServiceCommand.Stop -> requestCleanup(startId)
            null -> {
                statusStore.update(MockServiceStatus.InvalidCommand)
                requestCleanup(startId)
            }
        }
    }

    suspend fun destroy() {
        commandMutex.withLock {
            if (!cleanedUp) {
                publisher.stop()
                cleanedUp = true
            }
        }
    }

    private fun start(point: GeoPoint) {
        if (cleanedUp || cleanupJob?.isActive == true) return
        lastPoint = point
        callbacks.showPreparing(point)
        statusStore.update(MockServiceStatus.Starting(point))
        scope.launch {
            commandMutex.withLock {
                if (!cleanedUp) publisher.start(point)
            }
        }
    }

    private fun requestCleanup(startId: Int) {
        if (cleanupJob?.isActive == true || cleanedUp) return
        statusStore.update(MockServiceStatus.Stopping(lastPoint))
        cleanupJob = scope.launch {
            commandMutex.withLock {
                publisher.stop()
                cleanedUp = true
            }
            terminalFailure?.let { statusStore.update(MockServiceStatus.Failed(it)) }
                ?: statusStore.update(MockServiceStatus.Inactive)
            callbacks.removeForeground()
            callbacks.requestStopSelf(startId)
        }
    }

    private fun handlePublisherStatus(status: MockPublisherStatus) {
        when (status) {
            MockPublisherStatus.Idle -> Unit
            is MockPublisherStatus.Starting -> statusStore.update(MockServiceStatus.Starting(status.point))
            is MockPublisherStatus.Active -> {
                lastPoint = status.point
                statusStore.update(MockServiceStatus.Active(status.point))
                callbacks.showActive(status.point)
            }
            is MockPublisherStatus.Failed -> {
                terminalFailure = status.failure
                callbacks.logFailure(status.failure.cause)
                statusStore.update(MockServiceStatus.Failed(status.failure))
                requestCleanup(latestStartId)
            }
        }
    }

    interface Callbacks {
        fun showPreparing(point: GeoPoint)
        fun showActive(point: GeoPoint)
        fun removeForeground()
        fun requestStopSelf(startId: Int)
        fun logFailure(error: Throwable)
    }
}
