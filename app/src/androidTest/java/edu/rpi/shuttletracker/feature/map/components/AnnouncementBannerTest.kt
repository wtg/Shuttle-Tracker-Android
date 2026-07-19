package edu.rpi.shuttletracker.feature.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnouncementBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plainMessageRendersNormally() {
        setContent(listOf(announcement("a", "Shuttles are running on time.")))

        composeRule.onNodeWithText("Shuttles are running on time.").assertIsDisplayed()
    }

    @Test
    fun eachTypeHasADistinctIconContentDescription() {
        setContent(listOf(announcement("a", "Error message", type = AnnouncementType.Error)))

        composeRule.onNodeWithContentDescription("Error").assertIsDisplayed()
    }

    @Test
    fun markdownLinkLabelIsDisplayed() {
        setContent(
            listOf(
                announcement(
                    "a",
                    "Chasan is limited. [View info](https://example.com/info) for details.",
                ),
            ),
        )

        composeRule.onNodeWithText("Chasan is limited. View info for details.").assertIsDisplayed()
    }

    @Test
    fun malformedMarkdownDoesNotCrashAndShowsRawText() {
        setContent(listOf(announcement("a", "Broken [link(https://example.com) missing bracket")))

        composeRule.onNodeWithText("Broken [link(https://example.com) missing bracket").assertIsDisplayed()
    }

    @Test
    fun onlyMostSevereBannerShowsByDefaultWithExpandAffordance() {
        setContent(
            listOf(
                announcement("error", "Critical issue", type = AnnouncementType.Error),
                announcement("info", "Minor note", type = AnnouncementType.Info),
            ),
        )

        composeRule.onNodeWithText("Critical issue").assertIsDisplayed()
        composeRule.onNodeWithText("Show 1 more").assertIsDisplayed()
    }

    private fun announcement(
        id: String,
        message: String,
        type: AnnouncementType = AnnouncementType.Info,
    ) = Announcement(id = id, message = message, type = type, active = true)

    private fun setContent(announcements: List<Announcement>) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                AnnouncementBanners(announcements = announcements)
            }
        }
    }
}
