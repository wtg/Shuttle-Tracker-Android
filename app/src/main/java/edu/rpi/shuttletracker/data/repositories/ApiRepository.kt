package edu.rpi.shuttletracker.data.repositories

import com.haroldadmin.cnradapter.NetworkResponse
import dagger.Lazy
import edu.rpi.shuttletracker.data.models.AnalyticsFactory
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.network.ApiHelperImpl
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class ApiRepository
    @Inject
    constructor(
        private val apiHelper: ApiHelperImpl,
        private val userPreferencesRepository: Lazy<UserPreferencesRepository>,
        private val analyticsFactory: AnalyticsFactory,
    ) {
        fun observeBuses(pollMs: Long = 5_000L) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(apiHelper.getBuses())
                    delay(pollMs)
                }
            }

        fun observeEtas(pollMs: Long = 30_000L) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(apiHelper.getEtas())
                    delay(pollMs)
                }
            }

        suspend fun getRoutes() = apiHelper.getRoutes()

        suspend fun getAnnouncements() = apiHelper.getAnnouncements()

        suspend fun getSchedule() = apiHelper.getSchedule()

        suspend fun getAggregatedSchedule() = apiHelper.getAggregatedSchedule()

        suspend fun sendAnalytics(event: Event): NetworkResponse<Unit, ErrorResponse>? {
            if (!userPreferencesRepository.get().getAllowAnalytics().first()) return null

            val analytics = analyticsFactory.build(event)
            return apiHelper.addAnalytics(analytics)
        }

        suspend fun sendRegistrationToken(token: String) = apiHelper.sendRegistrationToken(token)
    }
