package edu.rpi.shuttletracker.data.network

import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import retrofit2.http.GET

interface ApiService {
    @GET("/buses")
    suspend fun getBuses(): List<Bus>

    @GET("/stops")
    suspend fun getStops(): List<Stop>
}
