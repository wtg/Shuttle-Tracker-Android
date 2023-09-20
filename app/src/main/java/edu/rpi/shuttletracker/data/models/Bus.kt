package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.util.Flatten

data class Bus(
    @Flatten("location.coordinate.latitude")
    val latitude: Double,

    @Flatten("location.coordinate.longitude")
    val longitude: Double,

    @SerializedName("id")
    val id: Int,

    @Transient
    val busIcon: String,

    @Flatten("location.date")
    val busDate: String,

)
