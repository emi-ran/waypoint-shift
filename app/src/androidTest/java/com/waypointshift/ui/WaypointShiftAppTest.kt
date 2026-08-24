package com.waypointshift.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.waypointshift.model.GeoPoint
import org.junit.Rule
import org.junit.Test

class WaypointShiftAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inactiveStateShowsMapFallbackManualCoordinatesStartAndSavedActions() {
        composeRule.setContent {
            WaypointShiftApp(
                state = MainUiState(
                    selectedPoint = GeoPoint.of(41.0082, 28.9784),
                    latitudeInput = "41.008200",
                    longitudeInput = "28.978400",
                    setupSeen = true,
                ),
            )
        }

        composeRule.onNodeWithText("Manual coordinates remain available if map tiles fail").assertIsDisplayed()
        composeRule.onNodeWithText("Latitude").assertTextContains("Latitude")
        composeRule.onNodeWithText("Longitude").assertTextContains("Longitude")
        composeRule.onNodeWithText("Start").assertIsEnabled()
        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeRule.onNodeWithText("Recents").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Setup help").assertIsDisplayed()
    }

    @Test
    fun activeStateAnnouncesStatusAndKeepsManualControlsAvailable() {
        composeRule.setContent {
            WaypointShiftApp(
                state = MainUiState(
                    selectedPoint = GeoPoint.of(41.0082, 28.9784),
                    latitudeInput = "41.008200",
                    longitudeInput = "28.978400",
                    setupSeen = true,
                    sessionState = SessionState.Active,
                ),
            )
        }

        composeRule.onNodeWithText("Mock location active").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsEnabled()
        composeRule.onNodeWithText("Apply").assertIsDisplayed()
    }
}
