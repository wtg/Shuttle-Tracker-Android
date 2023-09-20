package edu.rpi.shuttletracker.data.repositories

import edu.rpi.shuttletracker.data.network.ApiHelperImpl
import javax.inject.Inject

class ShuttleTrackerRepository @Inject constructor(
    private val apiHelper: ApiHelperImpl,
) {
    suspend fun getBuses() = apiHelper.getBuses()

    suspend fun getStops() = apiHelper.getStops()
}
