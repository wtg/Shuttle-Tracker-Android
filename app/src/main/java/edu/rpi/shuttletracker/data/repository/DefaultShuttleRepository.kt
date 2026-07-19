package edu.rpi.shuttletracker.data.repository

import edu.rpi.shuttletracker.data.remote.ShuttleRemoteDataSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class DefaultShuttleRepository
    @Inject
    constructor(
        private val remoteDataSource: ShuttleRemoteDataSource,
    ) : ShuttleRepository {
        override fun observeVehicleLocations(pollMs: Long) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteDataSource.getVehicleLocations())
                    delay(pollMs)
                }
            }

        override fun observeVehicleEtas(pollMs: Long) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteDataSource.getVehicleEtas())
                    delay(pollMs)
                }
            }

        override fun observeVehicleVelocities(pollMs: Long) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteDataSource.getVehicleVelocities())
                    delay(pollMs)
                }
            }

        override suspend fun getRoutes() = remoteDataSource.getRoutes()

        override suspend fun getAnnouncements() = remoteDataSource.getAnnouncements()

        override suspend fun getSchedule() = remoteDataSource.getSchedule()

        override suspend fun sendRegistrationToken(token: String) = remoteDataSource.sendRegistrationToken(token)
    }
