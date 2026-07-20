package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.AggregatedSchedule
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("locations")
    suspend fun getVehicleLocations(): NetworkResponse<Map<String, VehicleLocation>, ErrorResponse>

    @GET("etas")
    suspend fun getVehicleEtas(): NetworkResponse<Map<String, VehicleStopEta>, ErrorResponse>

    @GET("velocities")
    suspend fun getVehicleVelocities(): NetworkResponse<Map<String, VehicleVelocities>, ErrorResponse>

    @GET("routes")
    suspend fun getRoutes(): NetworkResponse<Map<String, Route>, ErrorResponse>

    @GET("announcements")
    suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse>

    @GET("schedule")
    suspend fun getSchedule(): NetworkResponse<Schedule, ErrorResponse>

    @GET("aggregated-schedule")
    suspend fun getAggregatedSchedule(): NetworkResponse<List<AggregatedSchedule>, ErrorResponse>

    @POST("analytics/entries")
    suspend fun addAnalytics(
        @Body analytics: Analytics,
    ): NetworkResponse<Unit, ErrorResponse>

    @POST("notifications/fcmdevices")
    suspend fun sendRegistrationToken(
        @Body token: String,
    ): NetworkResponse<Unit, ErrorResponse>
}
