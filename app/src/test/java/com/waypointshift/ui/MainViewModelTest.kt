package com.waypointshift.ui

import com.waypointshift.data.InMemoryLocationPreferencesStorage
import com.waypointshift.data.LocationRepositoryImpl
import com.waypointshift.mock.MockClientOperation
import com.waypointshift.mock.MockFailureKind
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
    fun `setup failure preserves selection and exposes setup action`() = runTest {
        val fixture = fixture()
        val point = GeoPoint.of(41.0, 29.0)
        fixture.viewModel.selectPoint(point)
        val cause = SecurityException("not selected")
        fixture.controller.status.value = MockServiceStatus.Failed(
            MockSessionFailure(
                kind = MockFailureKind.SETUP_REQUIRED,
                operation = MockClientOperation.ENABLE,
                cleanupFailed = false,
                cause = cause,
            ),
        )
        runCurrent()

        assertEquals(point, fixture.viewModel.uiState.value.selectedPoint)
        assertTrue(fixture.viewModel.uiState.value.showSetupAction)
        assertEquals("Select Waypoint Shift as mock location app, then retry", fixture.viewModel.uiState.value.errorMessage)
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

    private fun kotlinx.coroutines.test.TestScope.fixture(): Fixture {
        val repository = LocationRepositoryImpl(InMemoryLocationPreferencesStorage(), backgroundScope)
        val controller = FakeController()
        val viewModel = MainViewModel(
            repository = repository,
            serviceController = controller,
            scope = backgroundScope,
            nowEpochMillis = { 1_000L },
            favoriteId = { "favorite-1" },
        )
        return Fixture(repository, controller, viewModel)
    }

    private data class Fixture(
        val repository: LocationRepositoryImpl,
        val controller: FakeController,
        val viewModel: MainViewModel,
    )

    private class FakeController : MockServiceControl {
        override val status = MutableStateFlow<MockServiceStatus>(MockServiceStatus.Inactive)
        val starts = mutableListOf<GeoPoint>()
        val updates = mutableListOf<GeoPoint>()
        var stops = 0

        override fun start(point: GeoPoint) {
            starts += point
        }

        override fun update(point: GeoPoint) {
            updates += point
        }

        override fun stop() {
            stops++
        }
    }
}
