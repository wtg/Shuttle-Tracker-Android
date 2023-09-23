package edu.rpi.shuttletracker.data.network

import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop

interface ApiHelper {
    suspend fun getRunningBuses(): List<Bus>

    suspend fun getStops(): List<Stop>

    suspend fun getRoutes(): List<Route>

    suspend fun addBus(busNum: Int, bus: BoardBus)

    suspend fun getAllBuses(): List<Int>
}
