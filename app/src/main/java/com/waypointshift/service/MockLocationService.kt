package com.waypointshift.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.waypointshift.WaypointShiftApplication
import com.waypointshift.mock.MockLocationPublisher
import com.waypointshift.model.GeoPoint
import com.waypointshift.notifications.MockLocationNotificationFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class MockLocationService : Service(), MockServiceLifecycle.Callbacks {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private lateinit var notifications: MockLocationNotificationFactory
    private lateinit var lifecycle: MockServiceLifecycle

    override fun onCreate() {
        super.onCreate()
        val container = (application as WaypointShiftApplication).container
        val publisher = MockLocationPublisher(
            client = container.createMockLocationClient(),
            scope = serviceScope,
        )
        notifications = MockLocationNotificationFactory(this)
        notifications.createChannel()
        lifecycle = MockServiceLifecycle(
            publisher = publisher,
            statusStore = container.mockServiceStatus,
            scope = serviceScope,
            callbacks = this,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycle.handle(MockServiceCommand.fromIntent(intent), startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runBlocking { lifecycle.destroy() }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun showPreparing(point: GeoPoint) {
        startForeground(
            MockLocationNotificationFactory.NOTIFICATION_ID,
            notifications.preparing(point),
        )
    }

    override fun showActive(point: GeoPoint) {
        startForeground(
            MockLocationNotificationFactory.NOTIFICATION_ID,
            notifications.active(point),
        )
    }

    override fun removeForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun requestStopSelf(startId: Int) {
        stopSelfResult(startId)
    }

    override fun logFailure(error: Throwable) {
        Log.e(TAG, "Mock location session failed", error)
    }

    private companion object {
        const val TAG = "MockLocationService"
    }
}
