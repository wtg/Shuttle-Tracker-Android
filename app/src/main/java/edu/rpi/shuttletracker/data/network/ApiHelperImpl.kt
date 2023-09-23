package edu.rpi.shuttletracker.data.network

import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(private val apiService: ApiService) : ApiHelper {
    override suspend fun getRunningBuses(): List<Bus> = apiService.getRunningBuses()

    override suspend fun getStops(): List<Stop> = apiService.getStops()

    override suspend fun getRoutes(): List<Route> = apiService.getRoutes()

    override suspend fun addBus(busNum: Int, bus: BoardBus) = apiService.addBus(busNum, bus)

    override suspend fun getAllBuses(): List<Int> = apiService.getAllBuses().sorted()
}
