package com.waypointshift.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.flow.StateFlow

interface MockServiceControl {
    val status: StateFlow<MockServiceStatus>
    fun start(point: GeoPoint)
    fun update(point: GeoPoint)
    fun stop()
}

class MockServiceController(
    context: Context,
    private val statusStore: MockServiceStatusStore,
) : MockServiceControl {
    private val applicationContext = context.applicationContext

    override val status: StateFlow<MockServiceStatus> = statusStore.status

    override fun start(point: GeoPoint) {
        ContextCompat.startForegroundService(
            applicationContext,
            MockServiceCommand.startIntent(applicationContext, point),
        )
    }

    override fun update(point: GeoPoint) {
        applicationContext.startService(
            MockServiceCommand.updateIntent(applicationContext, point),
        )
    }

    override fun stop() {
        applicationContext.startService(MockServiceCommand.stopIntent(applicationContext))
    }
}
