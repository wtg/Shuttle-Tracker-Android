package edu.rpi.shuttletracker.data.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class AnnouncementTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")

    private fun announcement(
        id: String,
        type: AnnouncementType = AnnouncementType.Info,
        active: Boolean = true,
        expiresAt: Instant? = null,
        createdAt: Instant? = null,
    ) = Announcement(id, "message $id", type, active, expiresAt, createdAt)

    @Test
    fun `inactive announcements are not displayable`() {
        val announcement = announcement("a", active = false)

        assertThat(announcement.isDisplayable(now)).isFalse()
    }

    @Test
    fun `active announcement with no expiration is displayable`() {
        val announcement = announcement("a", active = true, expiresAt = null)

        assertThat(announcement.isDisplayable(now)).isTrue()
    }

    @Test
    fun `active announcement expiring in the future is displayable`() {
        val announcement = announcement("a", expiresAt = now.plusSeconds(60))

        assertThat(announcement.isDisplayable(now)).isTrue()
    }

    @Test
    fun `active announcement that already expired is not displayable`() {
        val announcement = announcement("a", expiresAt = now.minusSeconds(60))

        assertThat(announcement.isDisplayable(now)).isFalse()
    }

    @Test
    fun `displayable filters out inactive and expired announcements`() {
        val announcements =
            listOf(
                announcement("active", active = true),
                announcement("inactive", active = false),
                announcement("expired", expiresAt = now.minusSeconds(1)),
            )

        assertThat(announcements.displayable(now).map { it.id }).containsExactly("active")
    }

    @Test
    fun `severity sorts errors before warnings before info`() {
        val announcements =
            listOf(
                announcement("info", type = AnnouncementType.Info),
                announcement("error", type = AnnouncementType.Error),
                announcement("warning", type = AnnouncementType.Warning),
            )

        assertThat(announcements.displayable(now).map { it.id })
            .containsExactly("error", "warning", "info")
            .inOrder()
    }

    @Test
    fun `same severity sorts newest createdAt first`() {
        val announcements =
            listOf(
                announcement("older", createdAt = now.minusSeconds(120)),
                announcement("newer", createdAt = now.minusSeconds(10)),
            )

        assertThat(announcements.displayable(now).map { it.id })
            .containsExactly("newer", "older")
            .inOrder()
    }

    @Test
    fun `missing createdAt sorts deterministically after dated announcements of the same severity`() {
        val announcements =
            listOf(
                announcement("no-date", createdAt = null),
                announcement("dated", createdAt = now.minusSeconds(10)),
            )

        assertThat(announcements.displayable(now).map { it.id })
            .containsExactly("dated", "no-date")
            .inOrder()
    }

    @Test
    fun `multiple active announcements are all displayable`() {
        val announcements =
            listOf(
                announcement("first", type = AnnouncementType.Error),
                announcement("second", type = AnnouncementType.Warning),
                announcement("third", type = AnnouncementType.Info),
            )

        assertThat(announcements.displayable(now)).hasSize(3)
    }
}
