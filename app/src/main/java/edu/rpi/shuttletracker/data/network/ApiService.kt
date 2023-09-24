package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ApiService {
    @GET("buses")
    suspend fun getRunningBuses(): NetworkResponse<List<Bus>, ErrorResponse>

    @GET("stops")
    suspend fun getStops(): NetworkResponse<List<Stop>, ErrorResponse>

    @GET("routes")
    suspend fun getRoutes(): NetworkResponse<List<Route>, ErrorResponse>

    @PATCH("buses/{busNum}")
    suspend fun addBus(@Path("busNum") busNum: Int, @Body bus: BoardBus): NetworkResponse<Bus, ErrorResponse>

    @GET("buses/all")
    suspend fun getAllBuses(): NetworkResponse<List<Int>, ErrorResponse>
}
