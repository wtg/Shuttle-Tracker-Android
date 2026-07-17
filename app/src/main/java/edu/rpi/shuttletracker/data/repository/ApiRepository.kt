package edu.rpi.shuttletracker.data.repository

import dagger.Lazy
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.AnalyticsFactory
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.remote.RemoteShuttleDataSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class ApiRepository
    @Inject
    constructor(
        private val remoteShuttleDataSource: RemoteShuttleDataSource,
        private val userPreferencesRepository: Lazy<UserPreferencesRepository>,
        private val analyticsFactory: AnalyticsFactory,
    ) {
        fun observeVehicleLocations(pollMs: Long = 5_000L) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteShuttleDataSource.getVehicleLocations())
                    delay(pollMs)
                }
            }

        fun observeVehicleEtas(pollMs: Long = 30_000L) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteShuttleDataSource.getVehicleEtas())
                    delay(pollMs)
                }
            }

        fun observeVehicleVelocities(pollMs: Long = 30_000L) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteShuttleDataSource.getVehicleVelocities())
                    delay(pollMs)
                }
            }

        suspend fun getRoutes() = remoteShuttleDataSource.getRoutes()

        suspend fun getAnnouncements() = remoteShuttleDataSource.getAnnouncements()

        suspend fun getSchedule() = remoteShuttleDataSource.getSchedule()

        suspend fun getAggregatedSchedule() = remoteShuttleDataSource.getAggregatedSchedule()

        suspend fun sendAnalytics(event: Event): NetworkResult<Unit>? {
            if (!userPreferencesRepository.get().getAllowAnalytics().first()) return null

            val analytics = analyticsFactory.build(event)
            return remoteShuttleDataSource.addAnalytics(analytics)
        }

        suspend fun sendRegistrationToken(token: String) = remoteShuttleDataSource.sendRegistrationToken(token)
    }
