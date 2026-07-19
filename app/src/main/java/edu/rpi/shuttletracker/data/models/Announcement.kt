package edu.rpi.shuttletracker.data.models

import java.time.Instant

/**
 * Severity is ordered so [severityRank] can drive display sorting: errors first, info last.
 * */
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

/**
 * An absent or unparseable [Announcement.expiresAt] must not hide an otherwise active announcement.
 * */
fun Announcement.isDisplayable(now: Instant = Instant.now()): Boolean =
    active && (expiresAt == null || expiresAt.isAfter(now))

/**
 * Severity first, then newest [Announcement.createdAt]; missing dates sort last within their severity
 * while staying stable relative to each other.
 * */
val AnnouncementDisplayOrder: Comparator<Announcement> =
    compareBy<Announcement> { it.type.severityRank }
        .thenByDescending { it.createdAt ?: Instant.MIN }

fun List<Announcement>.displayable(now: Instant = Instant.now()): List<Announcement> =
    filter { it.isDisplayable(now) }.sortedWith(AnnouncementDisplayOrder)
