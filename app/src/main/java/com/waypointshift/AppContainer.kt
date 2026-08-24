package com.waypointshift

import android.content.Context
import com.waypointshift.mock.GoogleMockLocationClient
import com.waypointshift.mock.MockLocationClient
import com.waypointshift.service.MockServiceStatusStore

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val mockServiceStatus = MockServiceStatusStore()

    fun createMockLocationClient(): MockLocationClient =
        GoogleMockLocationClient(applicationContext)
}
