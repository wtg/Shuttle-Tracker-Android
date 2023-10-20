package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.util.Flatten

data class Analytics(
    @SerializedName("id")
    var id: String,

    @SerializedName("userID")
    var userID: String,

    @SerializedName("date")
    var date: String,

    @SerializedName("clientPlatform")
    var clientPlatform: String,

    @SerializedName("clientPlatformVersion")
    var clientPlatformVersion: String,

    @SerializedName("appVersion")
    var appVersion: String,

    @SerializedName("boardBusCount")
    var boardBusCount: String,

    @Flatten("userSettings::colorTheme")
    var colorTheme: String,

    @Flatten("userSettings::colorBlindMode")
    var colorBlindMode: Boolean,

    @Flatten("userSettings::logging")
    var logging: Boolean,

    @Flatten("userSettings::serverBaseURL")
    var serverBaseURL: String,

    @Flatten("eventType::colorBlindModeToggled::enabled")
    var colorBlindModeToggled: Boolean,
)
