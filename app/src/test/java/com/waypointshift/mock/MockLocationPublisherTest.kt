package com.waypointshift.mock

import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockLocationPublisherTest {
    @Test
    fun `start enables before immediate publish and repeats every second`() = runTest {
        val client = RecordingClient()
        val clock = SchedulerClock(testScheduler)
        val publisher = MockLocationPublisher(client, backgroundScope, clock)
        val point = GeoPoint.of(41.0082, 28.9784)

        publisher.start(point)
        assertEquals(listOf("enable", "publish:$point"), client.calls)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, client.publishCount)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, client.publishCount)
    }

    @Test
    fun `second start updates point without second enable or publication loop`() = runTest {
        val client = RecordingClient()
        val publisher = MockLocationPublisher(client, backgroundScope, SchedulerClock(testScheduler))
        val first = GeoPoint.of(41.0, 29.0)
        val second = GeoPoint.of(40.0, 28.0)

        publisher.start(first)
        publisher.start(second)
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(1, client.calls.count { it == "enable" })
        assertEquals(2, client.calls.count { it == "publish:$second" })
        assertEquals(MockPublisherStatus.Active(second), publisher.status.value)
    }

    @Test
    fun `stop cancels publication before disable and repeated stop is safe`() = runTest {
        val client = RecordingClient()
        val publisher = MockLocationPublisher(client, backgroundScope, SchedulerClock(testScheduler))

        publisher.start(GeoPoint.of(41.0, 29.0))
        publisher.stop()
        publisher.stop()
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(1, client.calls.count { it == "disable" })
        assertEquals(1, client.publishCount)
        assertEquals(MockPublisherStatus.Idle, publisher.status.value)
    }

    @Test
    fun `publish failure disables and prevents later publications`() = runTest {
        val failure = IllegalStateException("publish failed")
        val client = RecordingClient(failPublishNumber = 2, publishFailure = failure)
        val publisher = MockLocationPublisher(client, backgroundScope, SchedulerClock(testScheduler))

        publisher.start(GeoPoint.of(41.0, 29.0))
        advanceTimeBy(1_000)
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()

        val status = publisher.status.value as MockPublisherStatus.Failed
        assertSameFailure(failure, status.failure)
        assertEquals(1, client.calls.count { it == "disable" })
        assertEquals(2, client.publishCount)
    }

    @Test
    fun `cancellation during enable attempts disable exactly once`() = runTest {
        val client = SuspendingEnableClient()
        val publisher = MockLocationPublisher(client, backgroundScope, SchedulerClock(testScheduler))
        val start = async(start = CoroutineStart.UNDISPATCHED) {
            publisher.start(GeoPoint.of(41.0, 29.0))
        }

        start.cancel()
        runCurrent()

        assertTrue(start.isCancelled)
        assertEquals(1, client.disableCount)
    }

    private fun assertSameFailure(expected: Throwable, actual: MockSessionFailure) {
        assertEquals(MockFailureKind.PUBLISH_FAILED, actual.kind)
        assertEquals(MockClientOperation.PUBLISH, actual.operation)
        assertEquals(expected, actual.cause)
    }

    private class SchedulerClock(
        private val scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    ) : MockClock {
        override fun currentTimeMillis(): Long = scheduler.currentTime
        override fun elapsedRealtimeNanos(): Long = scheduler.currentTime * 1_000_000L
    }

    private class RecordingClient(
        private val failPublishNumber: Int? = null,
        private val publishFailure: Throwable = IllegalStateException(),
    ) : MockLocationClient {
        val calls = mutableListOf<String>()
        var publishCount = 0

        override suspend fun enable() {
            calls += "enable"
        }

        override suspend fun publish(point: GeoPoint, wallTimeMillis: Long, elapsedRealtimeNanos: Long) {
            publishCount++
            calls += "publish:$point"
            if (publishCount == failPublishNumber) throw publishFailure
        }

        override suspend fun disable() {
            calls += "disable"
        }
    }

    private class SuspendingEnableClient : MockLocationClient {
        var disableCount = 0

        override suspend fun enable() {
            kotlinx.coroutines.awaitCancellation()
        }

        override suspend fun publish(point: GeoPoint, wallTimeMillis: Long, elapsedRealtimeNanos: Long) = Unit

        override suspend fun disable() {
            disableCount++
        }
    }
}
