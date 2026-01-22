package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.AggregatedSchedule
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
    constructor(
        private val apiService: ApiService,
    ) : ApiHelper {
        override suspend fun getBuses(): Flow<NetworkResponse<Map<String, Bus>, ErrorResponse>> =
            flow {
                while (true) {
                    emit(apiService.getBuses())
                    delay(5000)
                }
            }

        override suspend fun getRoutes(): NetworkResponse<Map<String, Route>, ErrorResponse> = apiService.getRoutes()

        override suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse> =
            apiService.getAnnouncements()

        override suspend fun getSchedule(): NetworkResponse<Schedule, ErrorResponse> = apiService.getSchedule()

        override suspend fun getAggregatedSchedule(): NetworkResponse<List<AggregatedSchedule>, ErrorResponse> =
            apiService.getAggregatedSchedule()

        override suspend fun addAnalytics(analytics: Analytics): NetworkResponse<Unit, ErrorResponse> =
            apiService.addAnalytics(
                analytics,
            )

        override suspend fun sendRegistrationToken(token: String): NetworkResponse<Unit, ErrorResponse> =
            apiService.sendRegistrationToken(token)
    }
