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
) {
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
            Text("Manual coordinates remain available if map tiles fail")
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
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
}
