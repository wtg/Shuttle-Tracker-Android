package edu.rpi.shuttletracker.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UrlValidationTest {
    @Test
    fun `https urls are safe`() {
        assertThat(isSafeHttpUrl("https://example.com")).isTrue()
        assertThat(isSafeHttpUrl("HTTPS://example.com/path?query=1")).isTrue()
    }

    @Test
    fun `unencrypted and non-web schemes are rejected`() {
        assertThat(isSafeHttpUrl("http://example.com")).isFalse()
        assertThat(isSafeHttpUrl("javascript:alert(1)")).isFalse()
        assertThat(isSafeHttpUrl("ftp://example.com")).isFalse()
        assertThat(isSafeHttpUrl("intent://evil")).isFalse()
    }

    @Test
    fun `malformed urls are rejected without throwing`() {
        assertThat(isSafeHttpUrl("not a url at all")).isFalse()
        assertThat(isSafeHttpUrl("https://")).isFalse()
        assertThat(isSafeHttpUrl("")).isFalse()
    }
}
