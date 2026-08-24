package com.waypointshift.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.waypointshift.R
import com.waypointshift.model.GeoPoint
import com.waypointshift.service.MockServiceCommand

class MockLocationNotificationFactory(
    private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.mock_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.mock_notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun preparing(point: GeoPoint): Notification = build(
        title = context.getString(R.string.mock_notification_starting_title),
        point = point,
    )

    fun active(point: GeoPoint): Notification = build(
        title = context.getString(R.string.mock_notification_active_title),
        point = point,
    )

    private fun build(title: String, point: GeoPoint): Notification {
        val stopIntent = PendingIntent.getService(
            context,
            STOP_REQUEST_CODE,
            MockServiceCommand.stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val content = context.getString(
            R.string.mock_notification_coordinates,
            point.formattedLatitude,
            point.formattedLongitude,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_launcher,
                context.getString(R.string.mock_notification_stop_action),
                stopIntent,
            )
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mock_location_active"
        private const val STOP_REQUEST_CODE = 2001
    }
}
