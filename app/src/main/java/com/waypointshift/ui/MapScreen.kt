package com.waypointshift.ui

import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.waypointshift.model.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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
    val context = LocalContext.current
    val mapView = remember(context) {
        Configuration.getInstance().userAgentValue = "WaypointShift/0.1"
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(OsmGeoPoint(DEFAULT_CENTER.latitude, DEFAULT_CENTER.longitude))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    projection.fromPixels(event.x.toInt(), event.y.toInt())
                        .let { it as OsmGeoPoint }
                        .also { onMapPointSelected(GeoPoint.of(it.latitude, it.longitude)) }
                }
                false
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
    LaunchedEffect(displayedPoint) {
        mapView.overlays.removeAll { it is Marker }
        displayedPoint?.let { point ->
            Marker(mapView).apply {
                position = OsmGeoPoint(point.latitude, point.longitude)
                title = "Selected mock location"
                isDraggable = true
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(marker: Marker) {
                        onMarkerPreview(GeoPoint.of(marker.position.latitude, marker.position.longitude))
                    }
                    override fun onMarkerDragEnd(marker: Marker) {
                        onMarkerPreview(GeoPoint.of(marker.position.latitude, marker.position.longitude))
                        onMarkerDragEnd()
                    }
                    override fun onMarkerDragStart(marker: Marker) = Unit
                })
                mapView.overlays.add(this)
            }
            mapView.controller.animateTo(OsmGeoPoint(point.latitude, point.longitude))
        }
        mapView.invalidate()
    }
    DisposableEffect(mapView) { onDispose { mapView.onDetach() } }
}

private val DEFAULT_CENTER = GeoPoint.of(41.0082, 28.9784)
private const val DEFAULT_ZOOM = 12.0
