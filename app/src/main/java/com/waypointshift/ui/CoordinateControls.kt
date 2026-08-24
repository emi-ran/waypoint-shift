package com.waypointshift.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CoordinateControls(
    state: MainUiState,
    onInputsChanged: (String, String) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.latitudeInput,
                onValueChange = { onInputsChanged(it, state.longitudeInput) },
                modifier = Modifier.weight(1f),
                label = { Text("Latitude") },
                supportingText = state.latitudeError?.let { message -> { Text(message) } },
                isError = state.latitudeError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = state.longitudeInput,
                onValueChange = { onInputsChanged(state.latitudeInput, it) },
                modifier = Modifier.weight(1f),
                label = { Text("Longitude") },
                supportingText = state.longitudeError?.let { message -> { Text(message) } },
                isError = state.longitudeError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canApplyManual,
        ) {
            Text("Apply")
        }
    }
}
