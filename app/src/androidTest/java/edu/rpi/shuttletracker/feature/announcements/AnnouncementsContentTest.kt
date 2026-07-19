package edu.rpi.shuttletracker.feature.announcements

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnouncementsContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsProgress() {
        setContent(AnnouncementsUiState(isLoading = true))

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun contentStateShowsAnnouncement() {
        setContent(
            AnnouncementsUiState(
                announcements =
                    listOf(
                        Announcement(
                            subject = "Service update",
                            body = "Normal service",
                            rawStartTime = "2026-01-15T08:00:00-05:00",
                            rawEndTime = "2026-01-15T18:00:00-05:00",
                        ),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("Service update").assertIsDisplayed()
        composeRule.onNodeWithText("Normal service").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetryMessage() {
        setContent(
            AnnouncementsUiState(
                isLoading = false,
                serverError = NetworkError.Http(503, "maintenance"),
            ),
        )

        composeRule.onNodeWithText("Server error: maintenance").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    private fun setContent(state: AnnouncementsUiState) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                AnnouncementsContent(
                    uiState = state,
                    onBack = {},
                    onDismissError = {},
                    onRetry = {},
                )
            }
        }
    }
}
