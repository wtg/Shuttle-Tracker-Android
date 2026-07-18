package edu.rpi.shuttletracker.data.repository

import edu.rpi.shuttletracker.data.remote.RemoteShuttleDataSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class ApiRepository
    @Inject
    constructor(
        private val remoteShuttleDataSource: RemoteShuttleDataSource,
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

        suspend fun sendRegistrationToken(token: String) = remoteShuttleDataSource.sendRegistrationToken(token)
    }
