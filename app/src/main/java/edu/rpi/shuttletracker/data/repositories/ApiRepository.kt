package edu.rpi.shuttletracker.data.repositories

import com.haroldadmin.cnradapter.NetworkResponse
import dagger.Lazy
import edu.rpi.shuttletracker.data.models.AnalyticsFactory
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.network.ApiHelperImpl
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ApiRepository
    @Inject
    constructor(
        private val apiHelper: ApiHelperImpl,
        private val userPreferencesRepository: Lazy<UserPreferencesRepository>,
        private val analyticsFactory: AnalyticsFactory,
    ) {
        suspend fun getRunningBuses() = apiHelper.getRunningBuses()

        suspend fun getStops() = apiHelper.getStops()

        suspend fun getRoutes() = apiHelper.getRoutes()

        suspend fun addBus(
            busNum: Int,
            bus: BoardBus,
        ) = apiHelper.addBus(busNum, bus)

        suspend fun getAllBuses() = apiHelper.getAllBuses()

        suspend fun getAnnouncements() = apiHelper.getAnnouncements()

        suspend fun getSchedule() = apiHelper.getSchedule()

        suspend fun sendAnalytics(event: Event): NetworkResponse<Unit, ErrorResponse>? {
            if (!userPreferencesRepository.get().getAllowAnalytics().first()) return null

            val analytics = analyticsFactory.build(event)
            return apiHelper.addAnalytics(analytics)
        }

        suspend fun sendRegistrationToken(token: String) = apiHelper.sendRegistrationToken(token)

        suspend fun getApproachingBuses(
            latitude: Double,
            longitude: Double,
        ) = apiHelper.getApproachingBuses(latitude, longitude)
    }
