package edu.rpi.shuttletracker.data.mapper

import com.google.gson.Gson
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.EmptyEvent
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.remote.dto.AnalyticsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsDtoSerializationTest {
    private val gson = Gson()

    @Test
    fun `analytics settings serialize under userSettings`() {
        val json = gson.toJsonTree(analytics().toDto()).asJsonObject

        assertFalse(json.has("colorBlindMode"))
        assertFalse(json.has("logging"))
        assertFalse(json.has("serverBaseURL"))

        val settings = json.getAsJsonObject("userSettings")
        assertTrue(settings.get("colorBlindMode").asBoolean)
        assertFalse(settings.get("logging").asBoolean)
        assertEquals("https://example.com/", settings.get("serverBaseURL").asString)
    }

    @Test
    fun `value events serialize using the API nesting`() {
        val event =
            Event(
                colorBlindModeToggled = true,
                announcementViewed = "announcement-id",
                debugModeTogged = false,
                serverBaseURL = "https://server.example/",
                locationAuthorizationStatusChanged = 2,
                locationAccuracyAuthorizationDidChange = 1,
            )

        val eventJson = gson.toJsonTree(analytics(event).toDto()).asJsonObject.getAsJsonObject("eventType")

        assertTrue(eventJson.getAsJsonObject("colorBlindModeToggled").get("enabled").asBoolean)
        assertEquals("announcement-id", eventJson.getAsJsonObject("announcementViewed").get("id").asString)
        assertFalse(eventJson.getAsJsonObject("debugModeToggled").get("enabled").asBoolean)
        assertEquals("https://server.example/", eventJson.getAsJsonObject("serverBaseURLChanged").get("url").asString)
        assertEquals(
            2,
            eventJson.getAsJsonObject("locationAuthorizationStatusDidChange").get("authorizationStatus").asInt,
        )
        assertEquals(
            1,
            eventJson.getAsJsonObject("locationAccuracyAuthorizationDidChange").get("accuracyAuthorization").asInt,
        )
    }

    @Test
    fun `empty events serialize as empty objects and null events are omitted`() {
        val event = Event(coldLaunch = EmptyEvent, announcementsListOpened = EmptyEvent)
        val eventJson = gson.toJsonTree(analytics(event).toDto()).asJsonObject.getAsJsonObject("eventType")

        assertEquals(0, eventJson.getAsJsonObject("coldLaunch").size())
        assertEquals(0, eventJson.getAsJsonObject("announcementsListOpened").size())
        assertFalse(eventJson.has("permissionsSheetOpened"))
    }

    @Test
    fun `explicit analytics DTO round trips through Gson`() {
        val dto = analytics(Event(coldLaunch = EmptyEvent)).toDto()
        val decoded = gson.fromJson(gson.toJson(dto), AnalyticsDto::class.java)

        assertEquals(dto.id, decoded.id)
        assertEquals(dto.userSettings, decoded.userSettings)
        assertNotNull(decoded.event?.coldLaunch)
    }

    private fun analytics(event: Event? = null) =
        Analytics(
            id = "analytics-id",
            userID = "user-id",
            date = "2026-07-17T12:00:00Z",
            clientPlatform = "android",
            clientPlatformVersion = "16",
            appVersion = "1.0",
            colorBlindMode = true,
            logging = false,
            serverBaseURL = "https://example.com/",
            event = event,
        )
}
