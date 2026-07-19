package edu.rpi.shuttletracker.data.remote

import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.ScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleStopEtaDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleVelocitiesDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
    suspend fun getAnnouncements(): Response<List<AnnouncementDto>>

    @GET("schedule")
    suspend fun getSchedule(): Response<ScheduleDto>

    @POST("notifications/fcmdevices")
    suspend fun sendRegistrationToken(
        @Body token: String,
    ): Response<Unit>
}
