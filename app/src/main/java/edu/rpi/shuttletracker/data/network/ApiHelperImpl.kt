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
import javax.inject.Inject

class ApiHelperImpl
    @Inject
    constructor(
        private val apiService: ApiService,
    ) : ApiHelper {
        override suspend fun getVehicleLocations(): NetworkResponse<Map<String, VehicleLocation>, ErrorResponse> =
            apiService.getVehicleLocations()

        override suspend fun getVehicleEtas(): NetworkResponse<Map<String, VehicleStopEta>, ErrorResponse> =
            apiService.getVehicleEtas()

        override suspend fun getVehicleVelocities(): NetworkResponse<Map<String, VehicleVelocities>, ErrorResponse> =
            apiService.getVehicleVelocities()

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
