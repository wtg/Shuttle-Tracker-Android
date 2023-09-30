package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import kotlinx.coroutines.flow.Flow

interface ApiHelper {
    suspend fun getRunningBuses(): Flow<NetworkResponse<List<Bus>, ErrorResponse>>

    suspend fun getStops(): NetworkResponse<List<Stop>, ErrorResponse>

    suspend fun getRoutes(): NetworkResponse<List<Route>, ErrorResponse>

    suspend fun addBus(busNum: Int, bus: BoardBus): NetworkResponse<Unit, ErrorResponse>

    suspend fun getAllBuses(): NetworkResponse<List<Int>, ErrorResponse>

    suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse>
}
