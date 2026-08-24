package edu.rpi.shuttletracker.data.mapper

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.Locale

private val ANNOUNCEMENT_ZONE: ZoneId = ZoneId.of("America/New_York")

/** Accepts the API's padded or unpadded local timestamps, with optional seconds. */
private val FLEXIBLE_LOCAL_DATE_TIME: DateTimeFormatter =
    DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NORMAL)
        .appendLiteral('T')
        .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NORMAL)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NORMAL)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NORMAL)
        .optionalEnd()
        .toFormatter(Locale.US)

/** Parses offset timestamps first, then local API timestamps; malformed values return null. */
fun parseAnnouncementInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null

    parseOffsetOrZoned(raw)?.let { return it }
    parseFlexibleLocal(raw)?.let { return it }
    return null
}

private fun parseOffsetOrZoned(raw: String): Instant? =
    try {
        OffsetDateTime.parse(raw).toInstant()
    } catch (_: DateTimeParseException) {
        try {
            ZonedDateTime.parse(raw).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

private fun parseFlexibleLocal(raw: String): Instant? =
    try {
        LocalDateTime.parse(raw, FLEXIBLE_LOCAL_DATE_TIME).atZone(ANNOUNCEMENT_ZONE).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
