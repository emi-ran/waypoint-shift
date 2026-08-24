package com.waypointshift.service

import com.waypointshift.mock.MockSessionFailure
import com.waypointshift.model.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface MockServiceStatus {
    data object Inactive : MockServiceStatus
    data class Starting(val point: GeoPoint) : MockServiceStatus
    data class Active(val point: GeoPoint) : MockServiceStatus
    data class Stopping(val point: GeoPoint?) : MockServiceStatus
    data class Failed(val failure: MockSessionFailure) : MockServiceStatus
    data object InvalidCommand : MockServiceStatus
}

class MockServiceStatusStore {
    private val mutableStatus = MutableStateFlow<MockServiceStatus>(MockServiceStatus.Inactive)

    val status: StateFlow<MockServiceStatus> = mutableStatus.asStateFlow()

    fun update(status: MockServiceStatus) {
        mutableStatus.value = status
    }
}
