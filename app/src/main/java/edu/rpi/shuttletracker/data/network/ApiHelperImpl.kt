package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(private val apiService: ApiService) : ApiHelper {
    override suspend fun getRunningBuses(): Flow<NetworkResponse<List<Bus>, ErrorResponse>> = flow {
        while (true) {
            emit(apiService.getRunningBuses())
            delay(5000)
        }
    }

    override suspend fun getStops(): NetworkResponse<List<Stop>, ErrorResponse> =
        apiService.getStops()

    override suspend fun getRoutes(): NetworkResponse<List<Route>, ErrorResponse> =
        apiService.getRoutes()

    override suspend fun addBus(busNum: Int, bus: BoardBus): NetworkResponse<Unit, ErrorResponse> =
        apiService.addBus(busNum, bus)

    override suspend fun getAllBuses(): NetworkResponse<List<Int>, ErrorResponse> =
        apiService.getAllBuses()

    override suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse> =
        apiService.getAnnouncements()

    override suspend fun getSchedule(): NetworkResponse<List<Schedule>, ErrorResponse> =
        apiService.getSchedule()
}
