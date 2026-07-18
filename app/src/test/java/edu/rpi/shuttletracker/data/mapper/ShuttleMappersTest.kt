package edu.rpi.shuttletracker.data.mapper

import com.google.gson.Gson
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.StopDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.format.DateTimeParseException
import java.util.Calendar

class ShuttleMappersTest {
    @Test
    fun `vehicle location JSON uses the backend field names`() {
        val dto =
            Gson().fromJson(
                """{"name":"North","latitude":42.73,"longitude":-73.68,"speed_mph":5.0,"timestamp":"2026-01-15T08:00:00-05:00","heading_degrees":90}""",
                VehicleLocationDto::class.java,
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
    fun `announcement maps valid API timestamps`() {
        val dto =
            AnnouncementDto(
                subject = "Service update",
                body = "Normal service",
                rawStartTime = "2026-01-15T08:00:00-05:00",
                rawEndTime = "2026-01-15T18:00:00-05:00",
            )

        val announcement = dto.toModel()

        assertEquals(2026, announcement.startCalendar.get(Calendar.YEAR))
    }
}
