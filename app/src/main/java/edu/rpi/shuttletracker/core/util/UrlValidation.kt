package edu.rpi.shuttletracker.core.util

import java.net.URI
import java.net.URISyntaxException

/** Accepts only absolute HTTPS URLs before they are opened outside the app. */
fun isSafeHttpUrl(url: String): Boolean =
    try {
        val uri = URI(url)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    } catch (_: URISyntaxException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
