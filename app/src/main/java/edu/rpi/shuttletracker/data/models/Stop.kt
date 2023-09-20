package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.util.Flatten

data class Stop(

    @Flatten("coordinate.latitude")
    val latitude: Double,

    @Flatten("coordinate.longitude")
    val longitude: Double,

    @SerializedName("name")
    val name: String,
)
