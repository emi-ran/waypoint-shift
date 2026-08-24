package com.waypointshift.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun FavoriteLabelDialog(
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save favorite") },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.takeCodePoints(MAX_LABEL_CODE_POINTS) },
                label = { Text("Label (optional)") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(label); onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

internal fun String.takeCodePoints(maxCodePoints: Int): String {
    if (codePointCount(0, length) <= maxCodePoints) return this
    return substring(0, offsetByCodePoints(0, maxCodePoints))
}

internal const val MAX_LABEL_CODE_POINTS = 60
