package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ApiHelperImpl
    @Inject
    constructor(private val apiService: ApiService) : ApiHelper {
        override suspend fun getRunningBuses(): Flow<NetworkResponse<Map<String, Bus>, ErrorResponse>> =
            flow {
                while (true) {
                    emit(apiService.getRunningBuses())
                    delay(5000)
                }
            }

        override suspend fun getAllBuses(): Flow<NetworkResponse<Map<String, Bus>, ErrorResponse>> =
            flow {
                while (true) {
                    emit(apiService.getAllBuses())
                    delay(5000)
                }
            }

        override suspend fun getRoutes(): NetworkResponse<Map<String, Route>, ErrorResponse> = apiService.getRoutes()

        override suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse> =
            apiService.getAnnouncements()

        override suspend fun getSchedule(): NetworkResponse<List<Schedule>, ErrorResponse> = apiService.getSchedule()

        override suspend fun addAnalytics(analytics: Analytics): NetworkResponse<Unit, ErrorResponse> =
            apiService.addAnalytics(
                analytics,
            )

        override suspend fun sendRegistrationToken(token: String): NetworkResponse<Unit, ErrorResponse> =
            apiService.sendRegistrationToken(token)
    }
