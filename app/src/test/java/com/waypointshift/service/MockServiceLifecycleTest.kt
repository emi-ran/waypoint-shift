package com.waypointshift.service

import com.waypointshift.mock.MockClientOperation
import com.waypointshift.mock.MockFailureKind
import com.waypointshift.mock.MockPublisher
import com.waypointshift.mock.MockPublisherStatus
import com.waypointshift.mock.MockSessionFailure
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockServiceLifecycleTest {
    @Test
    fun `start shows foreground notification before publisher setup then reports active`() = runTest {
        val publisher = FakePublisher()
        val callbacks = RecordingCallbacks()
        val statusStore = MockServiceStatusStore()
        val lifecycle = MockServiceLifecycle(publisher, statusStore, backgroundScope, callbacks)
        val point = GeoPoint.of(41.0082, 28.9784)

        lifecycle.handle(MockServiceCommand.Start(point), startId = 1)

        assertEquals(listOf("preparing:$point"), callbacks.events)
        assertEquals(emptyList<GeoPoint>(), publisher.starts)
        runCurrent()
        assertEquals(listOf(point), publisher.starts)

        publisher.mutableStatus.value = MockPublisherStatus.Active(point)
        runCurrent()
        assertEquals("active:$point", callbacks.events.last())
        assertEquals(MockServiceStatus.Active(point), statusStore.status.value)
    }

    @Test
    fun `update reuses lifecycle and stop removes foreground after publisher cleanup`() = runTest {
        val publisher = FakePublisher()
        val callbacks = RecordingCallbacks()
        val lifecycle = MockServiceLifecycle(
            publisher,
            MockServiceStatusStore(),
            backgroundScope,
            callbacks,
        )
        val first = GeoPoint.of(41.0, 29.0)
        val second = GeoPoint.of(40.0, 28.0)

        lifecycle.handle(MockServiceCommand.Start(first), 1)
        lifecycle.handle(MockServiceCommand.Update(second), 2)
        runCurrent()
        lifecycle.handle(MockServiceCommand.Stop, 3)
        runCurrent()

        assertEquals(listOf(first, second), publisher.starts)
        assertEquals(1, publisher.stopCount)
        assertEquals("remove", callbacks.events[callbacks.events.lastIndex - 1])
        assertEquals("stop:3", callbacks.events.last())
    }

    @Test
    fun `invalid command and publisher failure use same idempotent cleanup path`() = runTest {
        val publisher = FakePublisher()
        val callbacks = RecordingCallbacks()
        val statusStore = MockServiceStatusStore()
        val lifecycle = MockServiceLifecycle(publisher, statusStore, backgroundScope, callbacks)

        lifecycle.handle(null, 4)
        lifecycle.handle(MockServiceCommand.Stop, 4)
        runCurrent()

        assertEquals(1, publisher.stopCount)
        assertEquals(1, callbacks.events.count { it == "remove" })

        val secondPublisher = FakePublisher()
        val secondCallbacks = RecordingCallbacks()
        val secondStatus = MockServiceStatusStore()
        MockServiceLifecycle(secondPublisher, secondStatus, backgroundScope, secondCallbacks)
        val cause = IllegalStateException("publish failed")
        val failure = MockSessionFailure(
            kind = MockFailureKind.PUBLISH_FAILED,
            operation = MockClientOperation.PUBLISH,
            cleanupFailed = false,
            cause = cause,
        )

        secondPublisher.mutableStatus.value = MockPublisherStatus.Failed(failure)
        runCurrent()

        assertEquals(1, secondPublisher.stopCount)
        assertEquals(MockServiceStatus.Failed(failure), secondStatus.status.value)
        assertEquals(listOf(cause), secondCallbacks.failures)
    }

    private class FakePublisher : MockPublisher {
        val mutableStatus = MutableStateFlow<MockPublisherStatus>(MockPublisherStatus.Idle)
        override val status: StateFlow<MockPublisherStatus> = mutableStatus
        val starts = mutableListOf<GeoPoint>()
        var stopCount = 0

        override suspend fun start(point: GeoPoint) {
            starts += point
        }

        override suspend fun stop() {
            stopCount++
            mutableStatus.value = MockPublisherStatus.Idle
        }
    }

    private class RecordingCallbacks : MockServiceLifecycle.Callbacks {
        val events = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()

        override fun showPreparing(point: GeoPoint) {
            events += "preparing:$point"
        }

        override fun showActive(point: GeoPoint) {
            events += "active:$point"
        }

        override fun removeForeground() {
            events += "remove"
        }

        override fun requestStopSelf(startId: Int) {
            events += "stop:$startId"
        }

        override fun logFailure(error: Throwable) {
            failures += error
        }
    }
}
