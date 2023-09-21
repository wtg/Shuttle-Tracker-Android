package edu.rpi.shuttletracker.data.network

import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ApiService {
    @GET("buses")
    suspend fun getBuses(): List<Bus>

    @GET("stops")
    suspend fun getStops(): List<Stop>

    @GET("routes")
    suspend fun getRoutes(): List<Route>

    @PATCH("buses/{busNum}")
    suspend fun addBus(@Path("busNum") busNum: Int, @Body bus: BoardBus)
}
