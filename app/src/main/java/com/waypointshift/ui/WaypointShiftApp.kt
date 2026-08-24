package com.waypointshift.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointShiftApp(
    state: MainUiState,
    onInputsChanged: (String, String) -> Unit = { _, _ -> },
    onApplyManual: () -> Unit = {},
    onMapPointSelected: (com.waypointshift.model.GeoPoint) -> Unit = {},
    onMarkerPreview: (com.waypointshift.model.GeoPoint) -> Unit = {},
    onMarkerDragEnd: () -> Unit = {},
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onHelp: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onRecents: () -> Unit = {},
    showSetup: Boolean = false,
    onOpenDeveloperOptions: () -> Unit = {},
    onSetupRetry: () -> Unit = {},
    onSetupDismiss: () -> Unit = {},
    showNotificationRationale: Boolean = false,
    onNotificationContinue: () -> Unit = {},
    onNotificationDismiss: () -> Unit = {},
    savedLocationsTab: SavedLocationsTab? = null,
    onSelectSavedLocation: (com.waypointshift.model.SavedLocation) -> Unit = {},
    onRenameFavorite: (String, String?) -> Unit = { _, _ -> },
    onDeleteFavorite: (String) -> Unit = {},
    onClearRecents: () -> Unit = {},
    onSavedLocationsDismiss: () -> Unit = {},
    onSaveFavorite: (String?) -> Unit = {},
    onGoToCurrentLocation: () -> Unit = {},
) {
    var showSaveFavorite by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waypoint Shift") },
                actions = {
                    TextButton(
                        onClick = onHelp,
                        modifier = Modifier.semantics { contentDescription = "Setup help" },
                    ) { Text("Help") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tap map or drag marker to select mock coordinate.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showAddressSearch = true }) { Text("Search address or place") }
                TextButton(onClick = onGoToCurrentLocation) { Text("Go to current location") }
            }
            Text("OpenStreetMap map and manual coordinates work without an API key.")
            MapScreen(
                selectedPoint = state.selectedPoint,
                previewPoint = state.previewPoint,
                onMapPointSelected = onMapPointSelected,
                onMarkerPreview = onMarkerPreview,
                onMarkerDragEnd = onMarkerDragEnd,
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )
            CoordinateControls(
                state = state,
                onInputsChanged = onInputsChanged,
                onApply = onApplyManual,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onFavorites) { Text("Favorites") }
                TextButton(onClick = onRecents) { Text("Recents") }
                TextButton(
                    onClick = { showSaveFavorite = true },
                    enabled = state.selectedPoint != null,
                ) { Text("Save favorite") }
            }
            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            val active = state.sessionState == SessionState.Active
            Text(
                text = if (active) "Mock location active" else "Mock location inactive",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.canStop) {
                Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Stop") }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canStart,
                ) { Text("Start") }
            }
        }
    }
    if (showSetup) {
        DeveloperOptionsSheet(
            onOpenDeveloperOptions = onOpenDeveloperOptions,
            onRetry = onSetupRetry,
            onDismiss = onSetupDismiss,
        )
    }
    if (showNotificationRationale) {
        NotificationPermissionDialog(
            onContinue = onNotificationContinue,
            onDismiss = onNotificationDismiss,
        )
    }
    savedLocationsTab?.let { tab ->
        SavedLocationsSheet(
            state = state,
            initialTab = tab,
            onSelect = onSelectSavedLocation,
            onRename = onRenameFavorite,
            onDelete = onDeleteFavorite,
            onClearRecents = onClearRecents,
            onDismiss = onSavedLocationsDismiss,
        )
    }
    if (showSaveFavorite) {
        FavoriteLabelDialog(
            onSave = onSaveFavorite,
            onDismiss = { showSaveFavorite = false },
        )
    }
    if (showAddressSearch) {
        AddressSearchDialog(
            onSelect = {
                onMapPointSelected(it)
                showAddressSearch = false
            },
            onDismiss = { showAddressSearch = false },
        )
    }
}
