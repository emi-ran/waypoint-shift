package com.waypointshift.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsSheet(
    onOpenDeveloperOptions: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Set up mock location app")
            Text("1. Open Developer Options.")
            Text("2. Choose Select mock location app.")
            Text("3. Select Waypoint Shift, return, then retry.")
            Text("Mock locations remain visible to Android and other apps.")
            Button(onClick = onOpenDeveloperOptions, modifier = Modifier.fillMaxWidth()) {
                Text("Open Developer Options")
            }
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Return and retry")
            }
        }
    }
}
