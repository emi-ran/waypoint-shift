package com.waypointshift.service

import android.content.Context
import android.content.Intent
import com.waypointshift.model.GeoPoint

sealed interface MockServiceCommand {
    data class Start(val point: GeoPoint) : MockServiceCommand
    data class Update(val point: GeoPoint) : MockServiceCommand
    data object Stop : MockServiceCommand

    companion object {
        const val ACTION_START = "com.waypointshift.action.START_MOCK_LOCATION"
        const val ACTION_UPDATE = "com.waypointshift.action.UPDATE_MOCK_LOCATION"
        const val ACTION_STOP = "com.waypointshift.action.STOP_MOCK_LOCATION"
        private const val EXTRA_LATITUDE = "com.waypointshift.extra.LATITUDE"
        private const val EXTRA_LONGITUDE = "com.waypointshift.extra.LONGITUDE"

        fun startIntent(context: Context, point: GeoPoint): Intent =
            pointIntent(context, ACTION_START, point)

        fun updateIntent(context: Context, point: GeoPoint): Intent =
            pointIntent(context, ACTION_UPDATE, point)

        fun stopIntent(context: Context): Intent =
            Intent(context, MockLocationService::class.java).setAction(ACTION_STOP)

        fun fromIntent(intent: Intent?): MockServiceCommand? {
            if (intent == null) return null
            val latitude = intent.takeIf { it.hasExtra(EXTRA_LATITUDE) }
                ?.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
            val longitude = intent.takeIf { it.hasExtra(EXTRA_LONGITUDE) }
                ?.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)
            return decode(intent.action, latitude, longitude)
        }

        fun decode(action: String?, latitude: Double?, longitude: Double?): MockServiceCommand? =
            when (action) {
                ACTION_START -> validatedPoint(latitude, longitude)?.let(::Start)
                ACTION_UPDATE -> validatedPoint(latitude, longitude)?.let(::Update)
                ACTION_STOP -> Stop
                else -> null
            }

        private fun pointIntent(context: Context, action: String, point: GeoPoint): Intent =
            Intent(context, MockLocationService::class.java)
                .setAction(action)
                .putExtra(EXTRA_LATITUDE, point.latitude)
                .putExtra(EXTRA_LONGITUDE, point.longitude)

        private fun validatedPoint(latitude: Double?, longitude: Double?): GeoPoint? {
            if (latitude == null || longitude == null) return null
            return runCatching { GeoPoint.of(latitude, longitude) }.getOrNull()
        }
    }
}
