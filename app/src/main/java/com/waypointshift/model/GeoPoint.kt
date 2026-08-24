package com.waypointshift.model

import java.util.Locale

@ConsistentCopyVisibility
data class GeoPoint private constructor(
    val latitude: Double,
    val longitude: Double,
) {
    val formattedLatitude: String
        get() = formatCoordinate(latitude)

    val formattedLongitude: String
        get() = formatCoordinate(longitude)

    companion object {
        private val dotDecimalPattern = Regex("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)")

        fun of(latitude: Double, longitude: Double): GeoPoint {
            validate(latitude, CoordinateComponent.LATITUDE, -90.0..90.0)
            validate(longitude, CoordinateComponent.LONGITUDE, -180.0..180.0)
            return GeoPoint(latitude, longitude)
        }

        fun parse(latitude: String, longitude: String): GeoPoint = of(
            latitude = parseComponent(latitude, CoordinateComponent.LATITUDE),
            longitude = parseComponent(longitude, CoordinateComponent.LONGITUDE),
        )

        private fun parseComponent(
            text: String,
            component: CoordinateComponent,
        ): Double {
            val normalized = text.trim()
            if (!dotDecimalPattern.matches(normalized)) {
                throw InvalidCoordinateException(component, CoordinateErrorReason.INVALID_FORMAT)
            }

            return normalized.toDoubleOrNull()
                ?: throw InvalidCoordinateException(component, CoordinateErrorReason.INVALID_FORMAT)
        }

        private fun validate(
            value: Double,
            component: CoordinateComponent,
            range: ClosedFloatingPointRange<Double>,
        ) {
            if (!value.isFinite()) {
                throw InvalidCoordinateException(component, CoordinateErrorReason.NOT_FINITE)
            }
            if (value !in range) {
                throw InvalidCoordinateException(component, CoordinateErrorReason.OUT_OF_RANGE)
            }
        }

        private fun formatCoordinate(value: Double): String =
            String.format(Locale.ROOT, "%.6f", if (value == 0.0) 0.0 else value)
    }
}

enum class CoordinateComponent {
    LATITUDE,
    LONGITUDE,
}

enum class CoordinateErrorReason {
    INVALID_FORMAT,
    NOT_FINITE,
    OUT_OF_RANGE,
}

class InvalidCoordinateException(
    val component: CoordinateComponent,
    val reason: CoordinateErrorReason,
) : IllegalArgumentException("Invalid ${component.name.lowercase()}: ${reason.name.lowercase()}")
