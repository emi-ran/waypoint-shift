package com.waypointshift.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.waypointshift.model.GeoPoint
import com.waypointshift.model.SavedLocation
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
        composeRule.onNodeWithText("Save favorite").assertIsDisplayed()
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

    @Test
    fun setupSheetExplainsTransparentDeveloperOptionsFlow() {
        composeRule.setContent {
            WaypointShiftApp(
                state = MainUiState(),
                showSetup = true,
            )
        }

        composeRule.onNodeWithText("1. Open Developer Options.").assertIsDisplayed()
        composeRule.onNodeWithText("2. Choose Select mock location app.").assertIsDisplayed()
        composeRule.onNodeWithText("3. Select Waypoint Shift, return, then retry.").assertIsDisplayed()
        composeRule.onNodeWithText("Mock locations remain visible to Android and other apps.").assertIsDisplayed()
        composeRule.onNodeWithText("Open Developer Options").assertIsDisplayed()
        composeRule.onNodeWithText("Return and retry").assertIsDisplayed()
    }

    @Test
    fun notificationRationaleBlocksStartWithExplicitExplanation() {
        composeRule.setContent {
            WaypointShiftApp(
                state = MainUiState(
                    selectedPoint = GeoPoint.of(41.0, 29.0),
                    latitudeInput = "41.000000",
                    longitudeInput = "29.000000",
                    setupSeen = true,
                ),
                showNotificationRationale = true,
            )
        }

        composeRule.onNodeWithText("Notification permission required").assertIsDisplayed()
        composeRule.onNodeWithText("Waypoint Shift requires notification permission before Start so active mocking stays clearly visible.").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun savedLocationsExposeSelectionAndValidRowActions() {
        val favorite = SavedLocation("fav-1", "Home", GeoPoint.of(41.0, 29.0), 1L)
        val recent = SavedLocation("recent-1", null, GeoPoint.of(40.0, 28.0), 2L)
        composeRule.setContent {
            WaypointShiftApp(
                state = MainUiState(favorites = listOf(favorite), recents = listOf(recent)),
                savedLocationsTab = SavedLocationsTab.Favorites,
            )
        }

        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Rename").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        composeRule.onNodeWithText("Clear recents").assertDoesNotExist()
    }
}
