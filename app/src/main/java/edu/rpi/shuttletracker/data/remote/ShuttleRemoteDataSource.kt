package edu.rpi.shuttletracker.data.remote

import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities

/** Remote shuttle operations, separated from Retrofit for testing. */
interface ShuttleRemoteDataSource {
    suspend fun getVehicleLocations(): NetworkResult<Map<String, VehicleLocation>>

    suspend fun getVehicleEtas(): NetworkResult<Map<String, VehicleStopEta>>

    suspend fun getVehicleVelocities(): NetworkResult<Map<String, VehicleVelocities>>

    suspend fun getRoutes(): NetworkResult<Map<String, Route>>

    suspend fun getAnnouncements(): NetworkResult<List<Announcement>>

    suspend fun getSchedule(): NetworkResult<Schedule>
}
