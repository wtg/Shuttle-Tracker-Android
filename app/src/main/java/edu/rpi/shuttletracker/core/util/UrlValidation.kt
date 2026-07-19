package edu.rpi.shuttletracker.core.util

import java.net.URI
import java.net.URISyntaxException

/**
 * Only `http`/`https` URLs with a host are safe to open; anything else (custom schemes, relative
 * paths, malformed URIs) is rejected rather than crashing or launching an unintended target.
 * */
fun isSafeHttpUrl(url: String): Boolean =
    try {
        val uri = URI(url)
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    } catch (_: URISyntaxException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
