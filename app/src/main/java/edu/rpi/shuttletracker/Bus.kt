package edu.rpi.shuttletracker

import android.security.keystore.BackendBusyException

class Bus (
    val latitude: Double,
    val longitude: Double,
    val id: Int,
    val busIcon: String,
    val busDate: String
    val busyException: BackendBusyException,


)