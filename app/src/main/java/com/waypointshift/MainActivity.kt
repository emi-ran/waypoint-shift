package com.waypointshift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waypointshift.ui.MainViewModel
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
                ) as T
            },
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.uiState.collectAsStateWithLifecycle()
            var showSetup by rememberSaveable { mutableStateOf(!state.setupSeen) }
            var showNotificationRationale by rememberSaveable { mutableStateOf(false) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                showNotificationRationale = !granted
                if (granted) mainViewModel.start()
            }

            fun requestTransparentStart() {
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
                    showSetup = showSetup || state.showSetupAction,
                    onOpenDeveloperOptions = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    },
                    onSetupRetry = {
                        mainViewModel.markSetupSeen()
                        showSetup = false
                        requestTransparentStart()
                    },
                    onSetupDismiss = { showSetup = false },
                    showNotificationRationale = showNotificationRationale,
                    onNotificationContinue = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onNotificationDismiss = { showNotificationRationale = false },
                )
            }
        }
    }
}
