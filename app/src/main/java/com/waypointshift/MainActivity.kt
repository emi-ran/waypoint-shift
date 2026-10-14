package com.waypointshift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waypointshift.ui.MainViewModel
import com.waypointshift.mock.MockLocationAccess
import com.waypointshift.ui.SavedLocationsTab
import com.waypointshift.ui.WaypointShiftApp
import com.waypointshift.ui.theme.WaypointShiftTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by lazy {
        val container = (application as WaypointShiftApplication).container
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                    repository = container.locationRepository,
                    serviceController = container.mockServiceController,
                    mockLocationAccessChecker = container.mockLocationAccessChecker,
                ) as T
            },
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.uiState.collectAsStateWithLifecycle()
            var showSetup by rememberSaveable { mutableStateOf(false) }
            var showNotificationRationale by rememberSaveable { mutableStateOf(false) }
            var showLocationDisabled by rememberSaveable { mutableStateOf(false) }
            var savedLocationsTab by rememberSaveable { mutableStateOf<SavedLocationsTab?>(null) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                showNotificationRationale = !granted
                if (granted) mainViewModel.start()
            }
            fun moveToCurrentLocation() {
                val locationManager = getSystemService(LocationManager::class.java)
                val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
                    .maxByOrNull { it.time }
                location?.let { mainViewModel.selectPoint(com.waypointshift.model.GeoPoint.of(it.latitude, it.longitude)) }
            }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { granted ->
                if (granted.values.any { it }) moveToCurrentLocation()
            }

            fun requestCurrentLocation() {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    moveToCurrentLocation()
                } else {
                    locationPermissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ))
                }
            }

            fun requestTransparentStart() {
                val locationManager = getSystemService(LocationManager::class.java)
                if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
                    showLocationDisabled = true
                    return
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mainViewModel.start()
                } else {
                    showNotificationRationale = true
                }
            }

            WaypointShiftTheme {
                WaypointShiftApp(
                    state = state,
                    onInputsChanged = mainViewModel::updateManualInputs,
                    onApplyManual = mainViewModel::applyManualInput,
                    onMapPointSelected = mainViewModel::selectPoint,
                    onMarkerPreview = mainViewModel::previewPoint,
                    onMarkerDragEnd = mainViewModel::commitPreviewPoint,
                    onStart = ::requestTransparentStart,
                    onStop = mainViewModel::stop,
                    onHelp = { showSetup = true },
                    onFavorites = { savedLocationsTab = SavedLocationsTab.Favorites },
                    onRecents = { savedLocationsTab = SavedLocationsTab.Recents },
                    showSetup = showSetup || state.showSetupAction ||
                        (!state.setupSeen && state.mockLocationAccess == MockLocationAccess.UNKNOWN),
                    onOpenDeveloperOptions = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    },
                    onSetupRetry = {
                        if (mainViewModel.confirmMockLocationSetup()) {
                            showSetup = false
                            requestTransparentStart()
                        }
                    },
                    onSetupDismiss = { showSetup = false },
                    showNotificationRationale = showNotificationRationale,
                    onNotificationContinue = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onNotificationDismiss = { showNotificationRationale = false },
                    savedLocationsTab = savedLocationsTab,
                    onSelectSavedLocation = mainViewModel::selectSavedLocation,
                    onRenameFavorite = mainViewModel::renameFavorite,
                    onDeleteFavorite = mainViewModel::deleteFavorite,
                    onClearRecents = mainViewModel::clearRecents,
                    onSavedLocationsDismiss = { savedLocationsTab = null },
                    onSaveFavorite = mainViewModel::saveFavorite,
                    onGoToCurrentLocation = ::requestCurrentLocation,
                )
                if (showLocationDisabled) {
                    AlertDialog(
                        onDismissRequest = { showLocationDisabled = false },
                        title = { Text("Turn on device location") },
                        text = { Text("Turn on Android Location before starting mock location. Waypoint Shift does not enable it automatically.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLocationDisabled = false
                                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }) { Text("Open Location Settings") }
                        },
                        dismissButton = { TextButton(onClick = { showLocationDisabled = false }) { Text("Cancel") } },
                    )
                }
            }
        }
    }
}
