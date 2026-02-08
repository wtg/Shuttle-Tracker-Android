package edu.rpi.shuttletracker.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.AggregatedSchedule
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.vehicle.VehicleLocation
import edu.rpi.shuttletracker.data.models.vehicle.VehicleStopEta

interface ApiHelper {
    suspend fun getVehicleLocations(): NetworkResponse<Map<String, VehicleLocation>, ErrorResponse>

    suspend fun getVehicleEtas(): NetworkResponse<Map<String, VehicleStopEta>, ErrorResponse>

    suspend fun getRoutes(): NetworkResponse<Map<String, Route>, ErrorResponse>

    suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse>

    suspend fun getSchedule(): NetworkResponse<Schedule, ErrorResponse>

    suspend fun getAggregatedSchedule(): NetworkResponse<List<AggregatedSchedule>, ErrorResponse>

    suspend fun addAnalytics(analytics: Analytics): NetworkResponse<Unit, ErrorResponse>

    suspend fun sendRegistrationToken(token: String): NetworkResponse<Unit, ErrorResponse>
}
