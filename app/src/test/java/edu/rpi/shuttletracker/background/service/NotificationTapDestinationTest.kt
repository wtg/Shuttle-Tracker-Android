package edu.rpi.shuttletracker.background.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationTapDestinationTest {
    @Test
    fun `no url opens the map`() {
        assertThat(resolveNotificationTapDestination(null)).isEqualTo(NotificationTapDestination.Map)
        assertThat(resolveNotificationTapDestination("")).isEqualTo(NotificationTapDestination.Map)
    }

    @Test
    fun `a valid https url opens externally`() {
        val destination = resolveNotificationTapDestination("https://example.com/info")

        assertThat(destination).isEqualTo(NotificationTapDestination.ExternalUrl("https://example.com/info"))
    }

    @Test
    fun `an unencrypted http url falls back to the map`() {
        assertThat(resolveNotificationTapDestination("http://example.com")).isEqualTo(NotificationTapDestination.Map)
    }

    @Test
    fun `an unsafe scheme falls back to the map`() {
        assertThat(resolveNotificationTapDestination("javascript:alert(1)")).isEqualTo(NotificationTapDestination.Map)
        assertThat(resolveNotificationTapDestination("intent://evil")).isEqualTo(NotificationTapDestination.Map)
    }

    @Test
    fun `a malformed url falls back to the map without throwing`() {
        assertThat(resolveNotificationTapDestination("not a url")).isEqualTo(NotificationTapDestination.Map)
    }
}
