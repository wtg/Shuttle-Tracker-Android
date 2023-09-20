package edu.rpi.shuttletracker.data.network

import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop

interface ApiHelper {
    suspend fun getBuses(): List<Bus>

    suspend fun getStops(): List<Stop>
}
