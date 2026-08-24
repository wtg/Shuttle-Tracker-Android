package edu.rpi.shuttletracker.background.service

import edu.rpi.shuttletracker.core.util.isSafeHttpUrl

/** Resolves all notification taps through the same URL safety check. */
sealed interface NotificationTapDestination {
    data object Map : NotificationTapDestination

    data class ExternalUrl(
        val url: String,
    ) : NotificationTapDestination
}

fun resolveNotificationTapDestination(url: String?): NotificationTapDestination =
    if (!url.isNullOrBlank() && isSafeHttpUrl(url)) {
        NotificationTapDestination.ExternalUrl(url)
    } else {
        NotificationTapDestination.Map
    }
