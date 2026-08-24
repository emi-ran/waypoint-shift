package com.waypointshift.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.flow.StateFlow

class MockServiceController(
    context: Context,
    private val statusStore: MockServiceStatusStore,
) {
    private val applicationContext = context.applicationContext

    val status: StateFlow<MockServiceStatus> = statusStore.status

    fun start(point: GeoPoint) {
        ContextCompat.startForegroundService(
            applicationContext,
            MockServiceCommand.startIntent(applicationContext, point),
        )
    }

    fun update(point: GeoPoint) {
        applicationContext.startService(
            MockServiceCommand.updateIntent(applicationContext, point),
        )
    }

    fun stop() {
        applicationContext.startService(MockServiceCommand.stopIntent(applicationContext))
    }
}
