package edu.rpi.shuttletracker.data.models

import java.time.Instant

/** Ordered from highest to lowest display priority. */
enum class AnnouncementType(
    val severityRank: Int,
) {
    Error(0),
    Warning(1),
    Info(2),
    ;

    companion object {
        fun fromApiValue(value: String?): AnnouncementType =
            when (value?.trim()?.lowercase()) {
                "error" -> Error
                "warning" -> Warning
                else -> Info
            }
    }
}

data class Announcement(
    val id: String,
    val message: String,
    val type: AnnouncementType,
    val active: Boolean,
    val expiresAt: Instant? = null,
    val createdAt: Instant? = null,
)

/** Missing expiration dates do not hide active announcements. */
fun Announcement.isDisplayable(now: Instant = Instant.now()): Boolean =
    active && (expiresAt == null || expiresAt.isAfter(now))

/** Sorts by severity, then newest creation time; missing dates sort last. */
val AnnouncementDisplayOrder: Comparator<Announcement> =
    compareBy<Announcement> { it.type.severityRank }
        .thenByDescending { it.createdAt ?: Instant.MIN }

fun List<Announcement>.displayable(now: Instant = Instant.now()): List<Announcement> =
    filter { it.isDisplayable(now) }.sortedWith(AnnouncementDisplayOrder)
