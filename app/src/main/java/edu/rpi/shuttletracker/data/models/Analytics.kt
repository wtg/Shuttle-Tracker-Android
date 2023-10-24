package edu.rpi.shuttletracker.data.models

import android.os.Build
import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.util.Flatten
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class Analytics(

    @SerializedName("id")
    var id: String = UUID.randomUUID().toString(),

    @SerializedName("userID")
    var userId: String,

    @SerializedName("date")
    var date: String = getCurrentFormattedDate(),

    @SerializedName("clientPlatform")
    var clientPlatform: String = "android",

    @SerializedName("clientPlatformVersion")
    var clientPlatformVersion: String = Build.VERSION.BASE_OS,

    @SerializedName("appVersion")
    var appVersion: String = BuildConfig.VERSION_NAME,

    @SerializedName("boardBusCount")
    var boardBusCount: Int = 0,

    @Flatten("userSettings::colorBlindMode")
    var colorBlindMode: Boolean,

    @Flatten("userSettings::logging")
    var logging: Boolean = true,

    @Flatten("userSettings::serverBaseURL")
    var serverBaseURL: String,

    @Flatten("eventType::boardBusActivated::manual")
    var manualBoardBus: Boolean,
) {

    /*    constructor(
        userId: String,
        manualBoardBus: Boolean,
        userPreferencesRepository: UserPreferencesRepository,
    ) : this(
        userId = userId,
        boardBusCount = 0,
        colorBlindMode = ,
        logging = true,
        serverBaseURL = ,
        manualBoardBus = manualBoardBus,
    )*/
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
