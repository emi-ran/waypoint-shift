package com.waypointshift

import android.content.Context
import com.waypointshift.data.LocationRepository
import com.waypointshift.data.LocationRepositoryImpl
import com.waypointshift.data.PreferencesLocationPreferencesStorage
import com.waypointshift.mock.GoogleMockLocationClient
import com.waypointshift.mock.MockLocationClient
import com.waypointshift.service.MockServiceControl
import com.waypointshift.service.MockServiceController
import com.waypointshift.service.MockServiceStatusStore
import kotlinx.coroutines.CoroutineScope

class AppContainer(
    context: Context,
    applicationScope: CoroutineScope,
) {
    private val applicationContext = context.applicationContext

    val mockServiceStatus = MockServiceStatusStore()
    val locationRepository: LocationRepository = LocationRepositoryImpl(
        storage = PreferencesLocationPreferencesStorage.fromContext(applicationContext),
        scope = applicationScope,
    )
    val mockServiceController: MockServiceControl = MockServiceController(
        context = applicationContext,
        statusStore = mockServiceStatus,
    )

    fun createMockLocationClient(): MockLocationClient =
        GoogleMockLocationClient(applicationContext)
}
