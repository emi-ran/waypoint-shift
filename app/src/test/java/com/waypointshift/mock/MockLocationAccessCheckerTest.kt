package com.waypointshift.mock

import android.app.AppOpsManager
import org.junit.Assert.assertEquals
import org.junit.Test

class MockLocationAccessCheckerTest {
    @Test
    fun `allowed app-op mode reports allowed`() {
        assertEquals(MockLocationAccess.ALLOWED, mockLocationAccessFromMode(AppOpsManager.MODE_ALLOWED))
    }

    @Test
    fun `non-allowed app-op modes report not allowed`() {
        listOf(
            AppOpsManager.MODE_DEFAULT,
            AppOpsManager.MODE_ERRORED,
            AppOpsManager.MODE_IGNORED,
        ).forEach { mode ->
            assertEquals(MockLocationAccess.NOT_ALLOWED, mockLocationAccessFromMode(mode))
        }
    }
}
