package edu.rpi.shuttletracker.data.network

import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(private val apiService: ApiService) : ApiHelper {
    override suspend fun getBuses(): List<Bus> = apiService.getBuses()

    override suspend fun getStops(): List<Stop> = apiService.getStops()
}
