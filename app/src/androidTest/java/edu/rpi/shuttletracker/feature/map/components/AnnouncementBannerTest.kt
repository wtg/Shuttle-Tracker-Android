package edu.rpi.shuttletracker.feature.map.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class AnnouncementBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plainMessageRendersNormally() {
        setStripContent(listOf(announcement("a", "Shuttles are running on time.")))

        composeRule.onNodeWithText("Shuttles are running on time.").assertIsDisplayed()
    }

    @Test
    fun eachTypeHasADistinctIconContentDescription() {
        setStripContent(listOf(announcement("a", "Error message", type = AnnouncementType.Error)))

        composeRule.onNodeWithContentDescription("Error").assertIsDisplayed()
    }

    @Test
    fun markdownLinkIsShownAsPlainTextInTheCollapsedStrip() {
        setStripContent(
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
        setStripContent(listOf(announcement("a", "Broken [link(https://example.com) missing bracket")))

        composeRule.onNodeWithText("Broken [link(https://example.com) missing bracket").assertIsDisplayed()
    }

    @Test
    fun multipleAnnouncementsShowMostSevereWithACount() {
        setStripContent(
            listOf(
                announcement("error", "Critical issue", type = AnnouncementType.Error),
                announcement("info", "Minor note", type = AnnouncementType.Info),
            ),
        )

        composeRule.onNodeWithText("Critical issue").assertIsDisplayed()
        composeRule.onNodeWithText("+1").assertIsDisplayed()
    }

    @Test
    fun tappingTheStripInvokesOnClick() {
        var clicked = false
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                AnnouncementStrip(
                    announcements = listOf(announcement("a", "Tap me")),
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Tap me").performClick()

        assert(clicked)
    }

    @Test
    fun theStripHasNoDismissAffordanceOnlyAChevron() {
        setStripContent(listOf(announcement("a", "No dismiss here")))

        composeRule.onAllNodesWithContentDescription("Dismiss announcement").assertCountEquals(0)
    }

    @Test
    fun sheetShowsFullMessageForEveryAnnouncement() {
        setSheetContent(
            announcements =
                listOf(
                    announcement("a", "Full detail message one", type = AnnouncementType.Warning),
                    announcement("b", "Full detail message two", type = AnnouncementType.Info),
                ),
        )

        composeRule.onNodeWithText("Full detail message one").assertIsDisplayed()
        composeRule.onNodeWithText("Full detail message two").assertIsDisplayed()
    }

    @Test
    fun updatedAtTextIsShownWhenProvided() {
        setSheetContent(
            announcements = listOf(announcement("a", "message")),
            updatedAt = Instant.parse("2026-01-15T12:00:00Z"),
        )

        composeRule.onNodeWithText("Announcements").assertIsDisplayed()
    }

    private fun announcement(
        id: String,
        message: String,
        type: AnnouncementType = AnnouncementType.Info,
    ) = Announcement(id = id, message = message, type = type, active = true)

    private fun setStripContent(announcements: List<Announcement>) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                AnnouncementStrip(announcements = announcements, onClick = {})
            }
        }
    }

    private fun setSheetContent(
        announcements: List<Announcement>,
        updatedAt: Instant? = null,
    ) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                AnnouncementSheet(
                    show = true,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    announcements = announcements,
                    updatedAt = updatedAt,
                    onDismiss = {},
                )
            }
        }
    }
}
