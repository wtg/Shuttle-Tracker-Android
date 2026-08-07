package edu.rpi.shuttletracker.testing.fakes

import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeShuttleRepository : ShuttleRepository {
    val vehicleLocations = MutableSharedFlow<NetworkResult<Map<String, VehicleLocation>>>(replay = 1)
    val vehicleEtas = MutableSharedFlow<NetworkResult<Map<String, VehicleStopEta>>>(replay = 1)
    val vehicleVelocities = MutableSharedFlow<NetworkResult<Map<String, VehicleVelocities>>>(replay = 1)
    val announcements = MutableSharedFlow<NetworkResult<List<Announcement>>>(replay = 1)

    var routesResult: NetworkResult<Map<String, Route>> = NetworkResult.Success(emptyMap())
    var scheduleResult: NetworkResult<Schedule>? = null

    var observeLocationsCalls = 0
    var observeEtasCalls = 0
    var observeVelocitiesCalls = 0
    var observeAnnouncementsCalls = 0
    var routesCalls = 0
    var scheduleCalls = 0

    override fun observeVehicleLocations(pollMs: Long): Flow<NetworkResult<Map<String, VehicleLocation>>> {
        observeLocationsCalls++
        return vehicleLocations
    }

    override fun observeVehicleEtas(pollMs: Long): Flow<NetworkResult<Map<String, VehicleStopEta>>> {
        observeEtasCalls++
        return vehicleEtas
    }

    override fun observeVehicleVelocities(pollMs: Long): Flow<NetworkResult<Map<String, VehicleVelocities>>> {
        observeVelocitiesCalls++
        return vehicleVelocities
    }

    override fun observeAnnouncements(pollMs: Long): Flow<NetworkResult<List<Announcement>>> {
        observeAnnouncementsCalls++
        return announcements
    }

    override suspend fun getRoutes(): NetworkResult<Map<String, Route>> {
        routesCalls++
        return routesResult
    }

    override suspend fun getSchedule(): NetworkResult<Schedule> {
        scheduleCalls++
        return checkNotNull(scheduleResult) { "Set scheduleResult before creating MapsViewModel" }
    }
}
