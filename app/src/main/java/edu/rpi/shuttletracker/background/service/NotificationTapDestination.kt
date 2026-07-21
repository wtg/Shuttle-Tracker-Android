package edu.rpi.shuttletracker.background.service

import edu.rpi.shuttletracker.core.util.isSafeHttpUrl

/**
 * Where a tapped push notification should take the user. Firebase Console messages may carry a
 * custom `url` data field for either the foreground path (our own PendingIntent extra) or the
 * background/terminated path (FCM copies its data payload onto the launcher intent's extras) -
 * both funnel through this same resolver so the safety check only lives in one place.
 * */
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
