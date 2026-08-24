package com.waypointshift.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.waypointshift.model.GeoPoint

@Composable
fun MapScreen(
    selectedPoint: GeoPoint?,
    previewPoint: GeoPoint?,
    onMapPointSelected: (GeoPoint) -> Unit,
    onMarkerPreview: (GeoPoint) -> Unit,
    onMarkerDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayedPoint = previewPoint ?: selectedPoint
    val initial = displayedPoint?.toLatLng() ?: DEFAULT_CENTER
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initial, DEFAULT_ZOOM)
    }
    var mapLoaded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            onMapClick = { latLng -> onMapPointSelected(latLng.toGeoPoint()) },
            onMapLoaded = { mapLoaded = true },
        ) {
            displayedPoint?.let { point ->
                DraggableSelectionMarker(
                    point = point,
                    onPreview = onMarkerPreview,
                    onDragEnd = onMarkerDragEnd,
                )
            }
        }
        if (!mapLoaded) {
            Text(
                text = "Map loading. Manual coordinates remain available.",
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    LaunchedEffect(selectedPoint) {
        selectedPoint?.let {
            cameraState.animate(CameraUpdateFactory.newLatLng(it.toLatLng()))
        }
    }
}

@Composable
private fun DraggableSelectionMarker(
    point: GeoPoint,
    onPreview: (GeoPoint) -> Unit,
    onDragEnd: () -> Unit,
) {
    val markerState = remember { MarkerState(position = point.toLatLng()) }
    LaunchedEffect(point) {
        if (!markerState.isDragging) markerState.position = point.toLatLng()
    }
    LaunchedEffect(markerState) {
        var wasDragging = false
        snapshotFlow { markerState.position to markerState.isDragging }
            .collect { (position, isDragging) ->
                if (isDragging) onPreview(position.toGeoPoint())
                if (wasDragging && !isDragging) {
                    onPreview(position.toGeoPoint())
                    onDragEnd()
                }
                wasDragging = isDragging
            }
    }
    Marker(
        state = markerState,
        draggable = true,
        title = "Selected mock location",
    )
}

private fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun LatLng.toGeoPoint(): GeoPoint = GeoPoint.of(latitude, longitude)

private val DEFAULT_CENTER = LatLng(41.0082, 28.9784)
private const val DEFAULT_ZOOM = 12f
