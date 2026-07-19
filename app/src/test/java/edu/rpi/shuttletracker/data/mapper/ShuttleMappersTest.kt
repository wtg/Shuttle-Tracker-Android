package edu.rpi.shuttletracker.data.mapper

import edu.rpi.shuttletracker.data.models.AnnouncementType
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementsResponseDto
import edu.rpi.shuttletracker.data.remote.dto.StopDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.format.DateTimeParseException

class ShuttleMappersTest {
    @Test
    fun `vehicle location JSON uses the backend field names`() {
        val dto =
            Json.decodeFromString<VehicleLocationDto>(
                """{"name":"North","latitude":42.73,"longitude":-73.68,"speed_mph":5.0,"timestamp":"2026-01-15T08:00:00-05:00","heading_degrees":90}""",
            )

        val location = dto.toModel()

        assertEquals("North", location.name)
        assertEquals(42.73, location.latitude, 0.0)
    }

    @Test
    fun `vehicle location rejects out of range coordinates`() {
        val dto =
            VehicleLocationDto(
                name = "Shuttle",
                latitude = 91.0,
                longitude = -73.68,
                speedMph = 5.0,
                timestamp = "2026-01-15T08:00:00-05:00",
                headingDegrees = 90,
            )

        assertThrows(IllegalArgumentException::class.java, dto::toModel)
    }

    @Test
    fun `vehicle location rejects malformed timestamps`() {
        val dto =
            VehicleLocationDto(
                name = "Shuttle",
                latitude = 42.73,
                longitude = -73.68,
                speedMph = 5.0,
                timestamp = "not-a-timestamp",
                headingDegrees = null,
            )

        assertThrows(DateTimeParseException::class.java, dto::toModel)
    }

    @Test
    fun `stop rejects missing longitude`() {
        val dto = StopDto(coordinates = listOf(42.73), offset = 0, name = "Union")

        assertThrows(IllegalArgumentException::class.java, dto::toModel)
    }

    @Test
    fun `wrapped announcements response decodes the announcements array`() {
        val json =
            """
            {
              "announcements": [
                {
                  "id": "chasan-weekday-hours",
                  "message": "Chasan stop is only available M-F 7am-5:30pm.",
                  "type": "info",
                  "active": true,
                  "expiresAt": "2026-12-31T23:59:59"
                }
              ]
            }
            """.trimIndent()

        val response = Json.decodeFromString<AnnouncementsResponseDto>(json)
        val announcements = response.toModel()

        assertEquals(1, announcements.size)
        assertEquals("chasan-weekday-hours", announcements.single().id)
        assertEquals(AnnouncementType.Info, announcements.single().type)
    }

    @Test
    fun `announcement mapping tolerates a missing createdAt`() {
        val dto = AnnouncementDto(id = "a", message = "m", type = "info", active = true, createdAt = null)

        assertNull(dto.toModel().createdAt)
    }

    @Test
    fun `announcement mapping tolerates a missing expiresAt`() {
        val dto = AnnouncementDto(id = "a", message = "m", type = "info", active = true, expiresAt = null)

        assertNull(dto.toModel().expiresAt)
    }

    @Test
    fun `announcement mapping accepts non-zero-padded local expiration`() {
        val dto =
            AnnouncementDto(id = "a", message = "m", type = "warning", active = false, expiresAt = "2026-1-28T23:59:59")

        assertEquals(Instant.parse("2026-01-29T04:59:59Z"), dto.toModel().expiresAt)
    }

    @Test
    fun `announcement mapping accepts an offset timestamp`() {
        val dto = AnnouncementDto(id = "a", message = "m", active = true, expiresAt = "2026-12-31T23:59:59-05:00")

        assertEquals(Instant.parse("2027-01-01T04:59:59Z"), dto.toModel().expiresAt)
    }

    @Test
    fun `announcement mapping interprets a local timestamp in America New_York`() {
        val dto = AnnouncementDto(id = "a", message = "m", active = true, expiresAt = "2026-12-31T23:59:59")

        assertEquals(Instant.parse("2027-01-01T04:59:59Z"), dto.toModel().expiresAt)
    }

    @Test
    fun `announcement mapping falls back to info for an unknown type`() {
        val dto = AnnouncementDto(id = "a", message = "m", type = "critical", active = true)

        assertEquals(AnnouncementType.Info, dto.toModel().type)
    }

    @Test
    fun `announcement mapping treats a malformed expiration as absent`() {
        val dto = AnnouncementDto(id = "a", message = "m", active = true, expiresAt = "not-a-date")

        assertNull(dto.toModel().expiresAt)
    }

    @Test
    fun `announcement mapping error type parses case-insensitively`() {
        assertEquals(
            AnnouncementType.Error,
            AnnouncementDto(id = "a", message = "m", type = "ERROR", active = true).toModel().type,
        )
        assertTrue(AnnouncementType.Error.severityRank < AnnouncementType.Warning.severityRank)
    }
}
