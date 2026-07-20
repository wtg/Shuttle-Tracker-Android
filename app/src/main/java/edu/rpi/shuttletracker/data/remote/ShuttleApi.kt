package edu.rpi.shuttletracker.data.remote

import edu.rpi.shuttletracker.data.remote.dto.AnnouncementsResponseDto
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.ScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleStopEtaDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleVelocitiesDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * The Retrofit description of the backend's REST endpoints (the Shubble API, see the repo README).
 * Each function is a raw HTTP call returning raw DTOs - nothing here should be called directly by
 * a feature; go through [edu.rpi.shuttletracker.data.repository.ShuttleRepository] instead.
 * */
interface ShuttleApi {
    @GET("locations")
    suspend fun getVehicleLocations(): Response<Map<String, VehicleLocationDto>>

    @GET("etas")
    suspend fun getVehicleEtas(): Response<Map<String, VehicleStopEtaDto>>

    @GET("velocities")
    suspend fun getVehicleVelocities(): Response<Map<String, VehicleVelocitiesDto>>

    @GET("routes")
    suspend fun getRoutes(): Response<Map<String, RouteDto>>

    @GET("announcements")
    suspend fun getAnnouncements(): Response<AnnouncementsResponseDto>

    @GET("schedule")
    suspend fun getSchedule(): Response<ScheduleDto>
}
