package com.waypointshift.model

data class SavedLocation(
    val id: String,
    val label: String? = null,
    val point: GeoPoint,
    val createdAtEpochMillis: Long,
    val lastUsedAtEpochMillis: Long = createdAtEpochMillis,
)
