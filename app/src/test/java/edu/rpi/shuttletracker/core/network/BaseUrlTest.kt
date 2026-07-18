package edu.rpi.shuttletracker.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlTest {
    @Test
    fun `adds the trailing slash required by Retrofit`() {
        assertEquals(
            "https://example.com/api/",
            normalizeBaseUrl("https://example.com/api"),
        )
    }

    @Test
    fun `accepts an existing secure base URL`() {
        assertEquals(
            "https://example.com/",
            normalizeBaseUrl("https://example.com/"),
        )
    }

    @Test
    fun `rejects unsupported or malformed URLs`() {
        assertNull(normalizeBaseUrl("not a url"))
        assertNull(normalizeBaseUrl("http://localhost:8080/"))
        assertNull(normalizeBaseUrl("ftp://example.com"))
    }

    @Test
    fun `rejects credentials queries and fragments`() {
        assertNull(normalizeBaseUrl("https://user:password@example.com"))
        assertNull(normalizeBaseUrl("https://example.com?key=value"))
        assertNull(normalizeBaseUrl("https://example.com#fragment"))
    }
}
