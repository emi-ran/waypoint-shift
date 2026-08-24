package com.waypointshift.ui

import com.waypointshift.data.InMemoryLocationPreferencesStorage
import com.waypointshift.data.LocationRepositoryImpl
import com.waypointshift.mock.MockClientOperation
import com.waypointshift.mock.MockFailureKind
import com.waypointshift.mock.MockLocationAccess
import com.waypointshift.mock.MockLocationAccessChecker
import com.waypointshift.mock.MockSessionFailure
import com.waypointshift.model.GeoPoint
import com.waypointshift.service.MockServiceControl
import com.waypointshift.service.MockServiceStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun `initial repository snapshot is inactive and never auto-starts`() = runTest {
        val fixture = fixture()
        runCurrent()

        assertEquals(SessionState.Inactive, fixture.viewModel.uiState.value.sessionState)
        assertTrue(fixture.controller.starts.isEmpty())
        assertTrue(fixture.viewModel.uiState.value.favorites.isEmpty())
    }

    @Test
    fun `selection canonicalizes fields and invalid manual input preserves selection`() = runTest {
        val fixture = fixture()
        val point = GeoPoint.of(41.0082, 28.9784)

        fixture.viewModel.selectPoint(point)
        fixture.viewModel.markSetupSeen()
        runCurrent()

        assertEquals("41.008200", fixture.viewModel.uiState.value.latitudeInput)
        assertEquals("28.978400", fixture.viewModel.uiState.value.longitudeInput)
        assertTrue(fixture.viewModel.uiState.value.canStart)

        fixture.viewModel.updateManualInputs("91", "28")
        fixture.viewModel.applyManualInput()

        assertEquals(point, fixture.viewModel.uiState.value.selectedPoint)
        assertEquals("Latitude must be between -90 and 90", fixture.viewModel.uiState.value.latitudeError)
        assertFalse(fixture.viewModel.uiState.value.canApplyManual)
    }

    @Test
    fun `allowed mock-location app-op avoids onboarding prompt and enables start`() = runTest {
        val fixture = fixture(access = MockLocationAccess.ALLOWED)
        val point = GeoPoint.of(41.0, 29.0)
        fixture.viewModel.selectPoint(point)
        runCurrent()

        assertFalse(fixture.viewModel.uiState.value.setupSeen)
        assertEquals(MockLocationAccess.ALLOWED, fixture.viewModel.uiState.value.mockLocationAccess)
        assertFalse(fixture.viewModel.uiState.value.showSetupAction)
        assertTrue(fixture.viewModel.uiState.value.canStart)

        fixture.viewModel.start()
        assertEquals(listOf(point), fixture.controller.starts)
    }

    @Test
    fun `denied mock-location app-op blocks service start with actionable diagnostic`() = runTest {
        val fixture = fixture(access = MockLocationAccess.NOT_ALLOWED)
        fixture.viewModel.selectPoint(GeoPoint.of(41.0, 29.0))
        runCurrent()

        assertEquals(SessionState.Failed, fixture.viewModel.uiState.value.sessionState)
        assertTrue(fixture.viewModel.uiState.value.showSetupAction)

        fixture.viewModel.start()

        assertTrue(fixture.controller.starts.isEmpty())
        assertTrue(fixture.viewModel.uiState.value.showSetupAction)
        assertEquals(
            "Android has not allowed mock location for Waypoint Shift. Open Developer Options, choose Waypoint Shift under Select mock location app, then return and retry.",
            fixture.viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `retry after app-op becomes allowed closes setup without repeating selection warning`() = runTest {
        val fixture = fixture(access = MockLocationAccess.NOT_ALLOWED)
        fixture.access.value = MockLocationAccess.ALLOWED

        assertTrue(fixture.viewModel.confirmMockLocationSetup())
        runCurrent()

        assertTrue(fixture.viewModel.uiState.value.setupSeen)
        assertFalse(fixture.viewModel.uiState.value.showSetupAction)
        assertNull(fixture.viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `unknown app-op keeps informational onboarding fallback`() = runTest {
        val fixture = fixture(access = MockLocationAccess.UNKNOWN)

        assertFalse(fixture.viewModel.uiState.value.setupSeen)
        assertEquals(MockLocationAccess.UNKNOWN, fixture.viewModel.uiState.value.mockLocationAccess)

        assertTrue(fixture.viewModel.confirmMockLocationSetup())
        runCurrent()
        assertTrue(fixture.viewModel.uiState.value.setupSeen)
    }

    @Test
    fun `marker preview does not update active service until drag commits`() = runTest {
        val fixture = fixture()
        val active = GeoPoint.of(41.0, 29.0)
        val preview = GeoPoint.of(40.0, 28.0)
        fixture.viewModel.selectPoint(active)
        fixture.controller.status.value = MockServiceStatus.Active(active)
        runCurrent()

        fixture.viewModel.previewPoint(preview)
        assertEquals(preview, fixture.viewModel.uiState.value.previewPoint)
        assertTrue(fixture.controller.updates.isEmpty())

        fixture.viewModel.commitPreviewPoint()
        assertEquals(preview, fixture.viewModel.uiState.value.selectedPoint)
        assertEquals(listOf(preview), fixture.controller.updates)
    }

    @Test
    fun `start records recent only after active status and active selection updates once`() = runTest {
        val fixture = fixture()
        val first = GeoPoint.of(41.0, 29.0)
        val second = GeoPoint.of(40.0, 28.0)
        fixture.viewModel.selectPoint(first)
        fixture.viewModel.markSetupSeen()
        runCurrent()

        fixture.viewModel.start()
        assertEquals(listOf(first), fixture.controller.starts)
        assertTrue(fixture.repository.recents.value.isEmpty())

        fixture.controller.status.value = MockServiceStatus.Active(first)
        runCurrent()
        assertEquals(first, fixture.repository.recents.value.single().point)

        fixture.viewModel.selectPoint(second)
        assertEquals(listOf(second), fixture.controller.updates)
        assertTrue(fixture.viewModel.uiState.value.canStop)
    }

    @Test
    fun `app-op failure preserves selection and exposes setup action`() = runTest {
        val fixture = fixture()
        val point = GeoPoint.of(41.0, 29.0)
        fixture.viewModel.selectPoint(point)
        val cause = SecurityException("not selected")
        fixture.controller.status.value = MockServiceStatus.Failed(
            MockSessionFailure(
                kind = MockFailureKind.APP_OP_NOT_ALLOWED,
                operation = MockClientOperation.ENABLE,
                cleanupFailed = false,
                cause = cause,
            ),
        )
        runCurrent()

        assertEquals(point, fixture.viewModel.uiState.value.selectedPoint)
        assertTrue(fixture.viewModel.uiState.value.showSetupAction)
        assertEquals(
            "Android has not allowed mock location for Waypoint Shift. Open Developer Options, choose Waypoint Shift under Select mock location app, then return and retry.",
            fixture.viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `Play services and Fused failures have distinct diagnostics`() = runTest {
        val fixture = fixture()

        fixture.controller.status.value = MockServiceStatus.Failed(
            failure(MockFailureKind.PLAY_SERVICES_UNAVAILABLE),
        )
        runCurrent()
        assertEquals(
            "Google Play services is missing, disabled, or outdated. Repair Google Play services, then retry.",
            fixture.viewModel.uiState.value.errorMessage,
        )

        fixture.controller.status.value = MockServiceStatus.Failed(
            failure(MockFailureKind.ENABLE_FAILED),
        )
        runCurrent()
        assertEquals(
            "Fused Location Provider could not enable mock mode. Check Google Play services and device restrictions, then retry.",
            fixture.viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `foreground-service start failure is reported separately`() = runTest {
        val fixture = fixture()
        fixture.controller.startFailure = IllegalStateException("background start blocked")
        fixture.viewModel.selectPoint(GeoPoint.of(41.0, 29.0))
        fixture.viewModel.markSetupSeen()
        runCurrent()

        fixture.viewModel.start()

        assertEquals(
            "Android could not start the visible mock-location service. Keep Waypoint Shift in the foreground and retry.",
            fixture.viewModel.uiState.value.errorMessage,
        )
        assertFalse(fixture.viewModel.uiState.value.showSetupAction)
    }

    @Test
    fun `favorite and recent actions call repository exactly once`() = runTest {
        val fixture = fixture()
        val point = GeoPoint.of(41.0, 29.0)
        fixture.viewModel.selectPoint(point)
        fixture.viewModel.saveFavorite("Home")
        runCurrent()

        val favorite = fixture.repository.favorites.value.single()
        assertEquals("Home", favorite.label)

        fixture.viewModel.renameFavorite(favorite.id, "New home")
        runCurrent()
        assertEquals("New home", fixture.repository.favorites.value.single().label)

        fixture.viewModel.deleteFavorite(favorite.id)
        fixture.repository.addRecent(point, 1L)
        fixture.viewModel.clearRecents()
        runCurrent()
        assertTrue(fixture.repository.favorites.value.isEmpty())
        assertTrue(fixture.repository.recents.value.isEmpty())
        assertNull(fixture.viewModel.uiState.value.errorMessage)
    }

    private fun kotlinx.coroutines.test.TestScope.fixture(
        access: MockLocationAccess = MockLocationAccess.UNKNOWN,
    ): Fixture {
        val repository = LocationRepositoryImpl(InMemoryLocationPreferencesStorage(), backgroundScope)
        val controller = FakeController()
        val accessChecker = FakeAccessChecker(access)
        val viewModel = MainViewModel(
            repository = repository,
            serviceController = controller,
            mockLocationAccessChecker = accessChecker,
            scope = backgroundScope,
            nowEpochMillis = { 1_000L },
            favoriteId = { "favorite-1" },
        )
        return Fixture(repository, controller, accessChecker, viewModel)
    }

    private data class Fixture(
        val repository: LocationRepositoryImpl,
        val controller: FakeController,
        val access: FakeAccessChecker,
        val viewModel: MainViewModel,
    )

    private class FakeAccessChecker(initial: MockLocationAccess) : MockLocationAccessChecker {
        var value = initial

        override fun check(): MockLocationAccess = value
    }

    private class FakeController : MockServiceControl {
        override val status = MutableStateFlow<MockServiceStatus>(MockServiceStatus.Inactive)
        val starts = mutableListOf<GeoPoint>()
        val updates = mutableListOf<GeoPoint>()
        var stops = 0
        var startFailure: RuntimeException? = null

        override fun start(point: GeoPoint) {
            startFailure?.let { throw it }
            starts += point
        }

        override fun update(point: GeoPoint) {
            updates += point
        }

        override fun stop() {
            stops++
        }
    }

    private fun failure(kind: MockFailureKind) = MockSessionFailure(
        kind = kind,
        operation = MockClientOperation.ENABLE,
        cleanupFailed = false,
        cause = IllegalStateException(kind.name),
    )
}
