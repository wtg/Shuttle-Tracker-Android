package edu.rpi.shuttletracker.data.repository

import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import kotlinx.coroutines.flow.Flow

/**
 * Provides shuttle data to the rest of the app.
 *
 * The interface lets tests replace the production repository with a small fake.
 */
interface ShuttleRepository {
    fun observeVehicleLocations(pollMs: Long = 5_000L): Flow<NetworkResult<Map<String, VehicleLocation>>>

    fun observeVehicleEtas(pollMs: Long = 30_000L): Flow<NetworkResult<Map<String, VehicleStopEta>>>

    fun observeVehicleVelocities(pollMs: Long = 30_000L): Flow<NetworkResult<Map<String, VehicleVelocities>>>

    fun observeAnnouncements(pollMs: Long = 300_000L): Flow<NetworkResult<List<Announcement>>>

    suspend fun getRoutes(): NetworkResult<Map<String, Route>>

    suspend fun getSchedule(): NetworkResult<Schedule>
}
