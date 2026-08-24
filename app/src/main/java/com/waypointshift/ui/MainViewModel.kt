package com.waypointshift.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waypointshift.data.LocationRepository
import com.waypointshift.mock.MockFailureKind
import com.waypointshift.model.CoordinateComponent
import com.waypointshift.model.CoordinateErrorReason
import com.waypointshift.model.GeoPoint
import com.waypointshift.model.InvalidCoordinateException
import com.waypointshift.model.SavedLocation
import com.waypointshift.service.MockServiceControl
import com.waypointshift.service.MockServiceStatus
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: LocationRepository,
    private val serviceController: MockServiceControl,
    scope: CoroutineScope? = null,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val favoriteId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val coroutineScope = scope ?: viewModelScope
    private val mutableUiState = MutableStateFlow(MainUiState())
    private var pendingRecent: GeoPoint? = null
    private var recentRecordJob: Job? = null

    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    init {
        coroutineScope.launch { repository.favorites.collect { update { copy(favorites = it) } } }
        coroutineScope.launch { repository.recents.collect { update { copy(recents = it) } } }
        coroutineScope.launch { repository.onboardingSeen.collect { update { copy(setupSeen = it) } } }
        coroutineScope.launch { serviceController.status.collect(::applyServiceStatus) }
    }

    fun selectPoint(point: GeoPoint) {
        val wasActive = mutableUiState.value.sessionState == SessionState.Active
        update {
            copy(
                selectedPoint = point,
                latitudeInput = point.formattedLatitude,
                longitudeInput = point.formattedLongitude,
                latitudeError = null,
                longitudeError = null,
            )
        }
        if (wasActive) serviceController.update(point)
    }

    fun updateManualInputs(latitude: String, longitude: String) {
        update {
            copy(
                latitudeInput = latitude,
                longitudeInput = longitude,
                latitudeError = validateComponent(latitude, CoordinateComponent.LATITUDE),
                longitudeError = validateComponent(longitude, CoordinateComponent.LONGITUDE),
            )
        }
    }

    fun applyManualInput() {
        val state = mutableUiState.value
        val point = runCatching { GeoPoint.parse(state.latitudeInput, state.longitudeInput) }
            .onFailure { error ->
                if (error is InvalidCoordinateException) {
                    update {
                        when (error.component) {
                            CoordinateComponent.LATITUDE -> copy(latitudeError = error.toMessage())
                            CoordinateComponent.LONGITUDE -> copy(longitudeError = error.toMessage())
                        }
                    }
                }
            }
            .getOrNull() ?: return
        selectPoint(point)
    }

    fun markSetupSeen() {
        coroutineScope.launch { repository.setOnboardingSeen(true) }
    }

    fun start() {
        val point = mutableUiState.value.selectedPoint ?: return
        if (!mutableUiState.value.canStart) return
        pendingRecent = point
        serviceController.start(point)
    }

    fun stop() {
        if (mutableUiState.value.canStop) serviceController.stop()
    }

    fun saveFavorite(label: String?) {
        val point = mutableUiState.value.selectedPoint ?: return
        coroutineScope.launch { repository.saveFavorite(favoriteId(), label, point, nowEpochMillis()) }
    }

    fun renameFavorite(id: String, label: String?) {
        val favorite = mutableUiState.value.favorites.firstOrNull { it.id == id } ?: return
        coroutineScope.launch { repository.saveFavorite(id, label, favorite.point, nowEpochMillis()) }
    }

    fun deleteFavorite(id: String) {
        coroutineScope.launch { repository.deleteFavorite(id) }
    }

    fun clearRecents() {
        coroutineScope.launch { repository.clearRecents() }
    }

    fun selectSavedLocation(location: SavedLocation) {
        selectPoint(location.point)
    }

    private fun applyServiceStatus(status: MockServiceStatus) {
        when (status) {
            MockServiceStatus.Inactive -> update {
                copy(sessionState = SessionState.Inactive, errorMessage = null, showSetupAction = false)
            }
            is MockServiceStatus.Starting -> update { copy(sessionState = SessionState.Starting) }
            is MockServiceStatus.Active -> {
                update { copy(sessionState = SessionState.Active, errorMessage = null, showSetupAction = false) }
                val point = pendingRecent?.takeIf { it == status.point }
                if (point != null && recentRecordJob?.isActive != true) {
                    pendingRecent = null
                    recentRecordJob = coroutineScope.launch { repository.addRecent(point, nowEpochMillis()) }
                }
            }
            is MockServiceStatus.Stopping -> update { copy(sessionState = SessionState.Stopping) }
            is MockServiceStatus.Failed -> {
                pendingRecent = null
                val setupRequired = status.failure.kind == MockFailureKind.SETUP_REQUIRED
                update {
                    copy(
                        sessionState = SessionState.Failed,
                        showSetupAction = setupRequired,
                        errorMessage = if (setupRequired) {
                            "Select Waypoint Shift as mock location app, then retry"
                        } else {
                            "Mock location stopped. Check Google Play services and retry"
                        },
                    )
                }
            }
            MockServiceStatus.InvalidCommand -> update {
                copy(sessionState = SessionState.Failed, errorMessage = "Invalid service command")
            }
        }
    }

    private fun validateComponent(text: String, component: CoordinateComponent): String? {
        val latitude = if (component == CoordinateComponent.LATITUDE) text else "0"
        val longitude = if (component == CoordinateComponent.LONGITUDE) text else "0"
        return runCatching { GeoPoint.parse(latitude, longitude) }
            .exceptionOrNull()
            ?.let { (it as? InvalidCoordinateException)?.toMessage() ?: "Invalid coordinate" }
    }

    private fun InvalidCoordinateException.toMessage(): String = when (component) {
        CoordinateComponent.LATITUDE -> when (reason) {
            CoordinateErrorReason.OUT_OF_RANGE -> "Latitude must be between -90 and 90"
            CoordinateErrorReason.NOT_FINITE -> "Latitude must be finite"
            CoordinateErrorReason.INVALID_FORMAT -> "Enter latitude using dot-decimal format"
        }
        CoordinateComponent.LONGITUDE -> when (reason) {
            CoordinateErrorReason.OUT_OF_RANGE -> "Longitude must be between -180 and 180"
            CoordinateErrorReason.NOT_FINITE -> "Longitude must be finite"
            CoordinateErrorReason.INVALID_FORMAT -> "Enter longitude using dot-decimal format"
        }
    }

    private inline fun update(transform: MainUiState.() -> MainUiState) {
        mutableUiState.value = mutableUiState.value.transform()
    }
}
