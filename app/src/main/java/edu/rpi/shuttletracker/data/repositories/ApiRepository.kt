package edu.rpi.shuttletracker.data.repositories

import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.network.ApiHelperImpl
import javax.inject.Inject

class ApiRepository @Inject constructor(
    private val apiHelper: ApiHelperImpl,
) {
    suspend fun getRunningBuses() = apiHelper.getRunningBuses()

    suspend fun getStops() = apiHelper.getStops()

    suspend fun getRoutes() = apiHelper.getRoutes()

    suspend fun addBus(busNum: Int, bus: BoardBus) = apiHelper.addBus(busNum, bus)

    suspend fun getAllBuses() = apiHelper.getAllBuses()

    suspend fun getAnnouncements() = apiHelper.getAnnouncements()

    suspend fun getSchedule() = apiHelper.getSchedule()

    suspend fun addAnalytics(analytics: Analytics) = apiHelper.addAnalytics(analytics)

    suspend fun sendRegistrationToken(token: String) = apiHelper.sendRegistrationToken(token)
}
