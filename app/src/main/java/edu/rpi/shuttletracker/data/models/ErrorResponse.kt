package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("reason")
    val reason: String,
)
