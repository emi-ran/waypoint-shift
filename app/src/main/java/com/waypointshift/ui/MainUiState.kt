package com.waypointshift.ui

import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
import com.waypointshift.mock.MockLocationAccess

enum class SessionState {
    Inactive,
    Starting,
    Active,
    Stopping,
    Failed,
}

data class MainUiState(
    val selectedPoint: GeoPoint? = null,
    val previewPoint: GeoPoint? = null,
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    val latitudeError: String? = null,
    val longitudeError: String? = null,
    val favorites: List<SavedLocation> = emptyList(),
    val recents: List<SavedLocation> = emptyList(),
    val setupSeen: Boolean = false,
    val mockLocationAccess: MockLocationAccess = MockLocationAccess.UNKNOWN,
    val sessionState: SessionState = SessionState.Inactive,
    val activeEngine: com.waypointshift.mock.MockLocationEngine? = null,
    val errorMessage: String? = null,
    val showSetupAction: Boolean = false,
) {
    val canApplyManual: Boolean
        get() = latitudeInput.isNotBlank() && longitudeInput.isNotBlank() &&
            latitudeError == null && longitudeError == null

    val canStart: Boolean
        get() = selectedPoint != null &&
            mockLocationAccess != MockLocationAccess.NOT_ALLOWED &&
            (setupSeen || mockLocationAccess == MockLocationAccess.ALLOWED) &&
            (sessionState == SessionState.Inactive || sessionState == SessionState.Failed)

    val canStop: Boolean
        get() = sessionState == SessionState.Starting || sessionState == SessionState.Active ||
            sessionState == SessionState.Stopping
}
