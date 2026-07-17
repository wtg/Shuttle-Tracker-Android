package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

data class Analytics(
    @SerializedName("id")
    val id: String,
    @SerializedName("userID")
    val userID: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("clientPlatform")
    val clientPlatform: String,
    @SerializedName("clientPlatformVersion")
    val clientPlatformVersion: String,
    @SerializedName("appVersion")
    val appVersion: String,
    val colorBlindMode: Boolean,
    val logging: Boolean,
    val serverBaseURL: String,
    @SerializedName("eventType")
    val event: Event?,
)

/**
 * https://github.com/wtg/Shuttle-Tracker-Server/wiki/Analytics#android
 * */
data class Event(
    val colorBlindModeToggled: Boolean? = null,
    // THIS IS NOT PLANNED ON BEING USED
    val announcementViewed: String? = null,
    // THIS IS NOT PLANNED ON BEING USED
    val debugModeTogged: Boolean? = null,
    val serverBaseURL: String? = null,
    // THIS IS NOT PLANNED ON BEING USED
    val locationAuthorizationStatusChanged: Int? = null,
    // THIS IS NOT PLANNED ON BEING USED
    val locationAccuracyAuthorizationDidChange: Int? = null,
    val coldLaunch: EmptyEvent? = null,
    val announcementsListOpened: EmptyEvent? = null,
    // THIS IS NOT PLANNED ON BEING USED
    val permissionsSheetOpened: EmptyEvent? = null,
)

/**
 * Just an empty object to pass an empty {} json
 * Since null gets removed
 * */
data object EmptyEvent

/**
 * This must be @Inject into a @AndroidEntryPoint to be used
 * */
class AnalyticsFactory
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) {
        fun build(event: Event? = null): Analytics =
            Analytics(
                id = UUID.randomUUID().toString(),
                userID = runBlocking { userPreferencesRepository.getUserId() },
                date = getCurrentFormattedDate(),
                clientPlatform = "android",
                clientPlatformVersion =
                    android.os.Build.VERSION.RELEASE
                        .toString(),
                appVersion = BuildConfig.VERSION_NAME,
                colorBlindMode = runBlocking { userPreferencesRepository.getColorBlindMode().first() },
                logging = false,
                serverBaseURL = runBlocking { userPreferencesRepository.getBaseUrl().first() },
                event = event,
            )

        companion object {
            /**
             *  Get the current date time in the format of ISO-8601 (e.g. 2021-11-12T22:44:55+00:00), excluding milliseconds.
             *  @return An ISO-8601 date string.
             */
            private fun getCurrentFormattedDate(): String {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC") // use UTC as default time zone

                return sdf.format(Date())
            }
        }
    }
