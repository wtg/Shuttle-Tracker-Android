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
import retrofit2.http.Url

interface ShuttleApi {
    @GET
    suspend fun getVehicleLocations(
        @Url url: String,
    ): Response<Map<String, VehicleLocationDto>>

    @GET
    suspend fun getVehicleEtas(
        @Url url: String,
    ): Response<Map<String, VehicleStopEtaDto>>

    @GET
    suspend fun getVehicleVelocities(
        @Url url: String,
    ): Response<Map<String, VehicleVelocitiesDto>>

    @GET
    suspend fun getRoutes(
        @Url url: String,
    ): Response<Map<String, RouteDto>>

    @GET
    suspend fun getAnnouncements(
        @Url url: String,
    ): Response<List<AnnouncementDto>>

    @GET
    suspend fun getSchedule(
        @Url url: String,
    ): Response<ScheduleDto>

    @POST
    suspend fun sendRegistrationToken(
        @Url url: String,
        @Body token: String,
    ): Response<Unit>
}
