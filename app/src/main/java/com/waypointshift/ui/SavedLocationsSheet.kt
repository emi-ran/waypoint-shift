package com.waypointshift.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waypointshift.model.SavedLocation

enum class SavedLocationsTab { Favorites, Recents }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsSheet(
    state: MainUiState,
    initialTab: SavedLocationsTab,
    onSelect: (SavedLocation) -> Unit,
    onRename: (String, String?) -> Unit,
    onDelete: (String) -> Unit,
    onClearRecents: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    var renameTarget by remember { mutableStateOf<SavedLocation?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedLocation?>(null) }
    var label by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { tab = SavedLocationsTab.Favorites }) { Text("Favorites") }
                TextButton(onClick = { tab = SavedLocationsTab.Recents }) { Text("Recents") }
            }
            val locations = if (tab == SavedLocationsTab.Favorites) state.favorites else state.recents
            if (locations.isEmpty()) Text("No saved locations")
            locations.forEach { location ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(location); onDismiss() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(location.label ?: "Recent coordinate")
                        Text("${location.point.formattedLatitude}, ${location.point.formattedLongitude}")
                    }
                    if (tab == SavedLocationsTab.Favorites) {
                        TextButton(onClick = { renameTarget = location; label = location.label.orEmpty() }) {
                            Text("Rename")
                        }
                        TextButton(onClick = { deleteTarget = location }) { Text("Delete") }
                    }
                }
            }
            if (tab == SavedLocationsTab.Recents && state.recents.isNotEmpty()) {
                TextButton(onClick = onClearRecents) { Text("Clear recents") }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename favorite") },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.takeCodePoints(MAX_LABEL_CODE_POINTS) },
                    label = { Text("Label") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target.id, label)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete favorite?") },
            text = { Text("This removes ${target.label ?: "this favorite"} from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}
