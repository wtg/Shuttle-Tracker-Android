package edu.rpi.shuttletracker.feature.map.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageSegmentTest {
    @Test
    fun `plain message with no markdown is a single text segment`() {
        val segments = parseMessageSegments("Shuttles are running on time.")

        assertThat(segments).containsExactly(MessageSegment.PlainText("Shuttles are running on time."))
    }

    @Test
    fun `extracts a single link surrounded by text`() {
        val segments =
            parseMessageSegments(
                "Chasan is limited. [View RPI Shuttle Info](https://administration.rpi.edu/info) for details.",
            )

        assertThat(segments)
            .containsExactly(
                MessageSegment.PlainText("Chasan is limited. "),
                MessageSegment.Link("View RPI Shuttle Info", "https://administration.rpi.edu/info"),
                MessageSegment.PlainText(" for details."),
            ).inOrder()
    }

    @Test
    fun `extracts multiple links from the same message`() {
        val segments = parseMessageSegments("See [A](https://a.example) and [B](https://b.example) now.")

        val links = segments.filterIsInstance<MessageSegment.Link>()
        assertThat(links)
            .containsExactly(
                MessageSegment.Link("A", "https://a.example"),
                MessageSegment.Link("B", "https://b.example"),
            ).inOrder()
    }

    @Test
    fun `malformed markdown with an unmatched bracket does not crash and is left as text`() {
        val segments = parseMessageSegments("Broken [link(https://example.com) missing bracket")

        assertThat(segments).containsExactly(
            MessageSegment.PlainText("Broken [link(https://example.com) missing bracket"),
        )
    }

    @Test
    fun `empty label falls back to the URL as the label`() {
        val segments = parseMessageSegments("[](https://example.com)")

        assertThat(segments).containsExactly(MessageSegment.Link("https://example.com", "https://example.com"))
    }
}
