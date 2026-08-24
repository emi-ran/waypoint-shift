package com.waypointshift.mock

import android.app.AppOpsManager
import android.content.Context

enum class MockLocationAccess {
    ALLOWED,
    NOT_ALLOWED,
    UNKNOWN,
}

fun interface MockLocationAccessChecker {
    fun check(): MockLocationAccess
}

class AndroidMockLocationAccessChecker(context: Context) : MockLocationAccessChecker {
    private val applicationContext = context.applicationContext
    private val appOpsManager = applicationContext.getSystemService(AppOpsManager::class.java)

    override fun check(): MockLocationAccess = try {
        mockLocationAccessFromMode(
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                applicationContext.applicationInfo.uid,
                applicationContext.packageName,
            ),
        )
    } catch (_: SecurityException) {
        MockLocationAccess.UNKNOWN
    } catch (_: IllegalArgumentException) {
        MockLocationAccess.UNKNOWN
    }
}

internal fun mockLocationAccessFromMode(mode: Int): MockLocationAccess =
    if (mode == AppOpsManager.MODE_ALLOWED) {
        MockLocationAccess.ALLOWED
    } else {
        MockLocationAccess.NOT_ALLOWED
    }
