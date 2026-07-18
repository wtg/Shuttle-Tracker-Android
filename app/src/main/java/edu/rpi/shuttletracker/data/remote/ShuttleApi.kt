package edu.rpi.shuttletracker.data.remote

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.ErrorResponse
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.ScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleStopEtaDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleVelocitiesDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ShuttleApi {
    @GET("locations")
    suspend fun getVehicleLocations(): NetworkResponse<Map<String, VehicleLocationDto>, ErrorResponse>

    @GET("etas")
    suspend fun getVehicleEtas(): NetworkResponse<Map<String, VehicleStopEtaDto>, ErrorResponse>

    @GET("velocities")
    suspend fun getVehicleVelocities(): NetworkResponse<Map<String, VehicleVelocitiesDto>, ErrorResponse>

    @GET("routes")
    suspend fun getRoutes(): NetworkResponse<Map<String, RouteDto>, ErrorResponse>

    @GET("announcements")
    suspend fun getAnnouncements(): NetworkResponse<List<AnnouncementDto>, ErrorResponse>

    @GET("schedule")
    suspend fun getSchedule(): NetworkResponse<ScheduleDto, ErrorResponse>

    @POST("notifications/fcmdevices")
    suspend fun sendRegistrationToken(
        @Body token: String,
    ): NetworkResponse<Unit, ErrorResponse>
}
