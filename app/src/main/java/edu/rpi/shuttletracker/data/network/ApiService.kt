package edu.rpi.shuttletracker.data.network

import com.google.gson.JsonObject
import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Schedule
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("locations")
    suspend fun getRunningBuses(): NetworkResponse<Map<String, Bus>, ErrorResponse>

    @GET("locations")
    suspend fun getAllBuses(): NetworkResponse<Map<String, Bus>, ErrorResponse>

    @GET("routes")
    suspend fun getRoutesRaw(): NetworkResponse<JsonObject, ErrorResponse>

    @PATCH("buses/{busNum}")
    suspend fun addBus(
        @Path("busNum") busNum: Int,
        @Body bus: BoardBus,
    ): NetworkResponse<Unit, ErrorResponse>

    @GET("announcements")
    suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse>

    @GET("schedule")
    suspend fun getSchedule(): NetworkResponse<List<Schedule>, ErrorResponse>

    @POST("analytics/entries")
    suspend fun addAnalytics(
        @Body analytics: Analytics,
    ): NetworkResponse<Unit, ErrorResponse>

    @POST("notifications/fcmdevices")
    suspend fun sendRegistrationToken(
        @Body token: String,
    ): NetworkResponse<Unit, ErrorResponse>
}
