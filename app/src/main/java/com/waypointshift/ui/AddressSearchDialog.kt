package com.waypointshift.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.waypointshift.model.GeoPoint
import com.waypointshift.search.NominatimSearchClient
import com.waypointshift.search.PlaceSearchResult
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun AddressSearchDialog(
    onSelect: (GeoPoint) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val client = remember { NominatimSearchClient() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search address or place") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Address, city, landmark") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    enabled = query.trim().length >= 3 && !loading,
                    onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            runCatching { client.search(query) }
                                .onSuccess { results = it }
                                .onFailure { error = "Search unavailable. Check internet and retry." }
                            loading = false
                        }
                    },
                ) { Text("Search") }
                if (loading) CircularProgressIndicator()
                error?.let { Text(it) }
                if (!loading && results.isEmpty() && query.trim().length >= 3 && error == null) {
                    Text("Search to see up to five OpenStreetMap results.")
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(results) { result ->
                        Text(
                            text = result.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(result.point) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
