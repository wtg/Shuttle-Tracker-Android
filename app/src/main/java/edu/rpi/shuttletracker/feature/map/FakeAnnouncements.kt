package edu.rpi.shuttletracker.feature.map

import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import java.time.Instant

/**
 * Sample banners covering every severity and a Markdown link, for exercising the map banner UI
 * when the shuttle API has nothing to show (e.g. summer break). Only reachable through the
 * developer menu's "Simulate announcements" toggle.
 * */
object FakeAnnouncements {
    fun sample(now: Instant = Instant.now()): List<Announcement> =
        listOf(
            Announcement(
                id = "fake-error",
                message = "Shuttles will not run or run with limited capacity until the weather improves.",
                type = AnnouncementType.Error,
                active = true,
                createdAt = now.minusSeconds(60),
            ),
            Announcement(
                id = "fake-warning",
                message = "Due to snowy roads, expect delays on West route shuttles.",
                type = AnnouncementType.Warning,
                active = true,
                createdAt = now.minusSeconds(120),
            ),
            Announcement(
                id = "fake-info",
                message =
                    "Chasan stop is only available M-F 7am-5:30pm. " +
                        "[View RPI Shuttle Info](https://administration.rpi.edu/parking-transportation/rensselaer-shuttle)",
                type = AnnouncementType.Info,
                active = true,
                createdAt = now.minusSeconds(180),
            ),
        )
}
