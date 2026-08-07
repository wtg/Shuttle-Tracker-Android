package edu.rpi.shuttletracker.data.repository

import edu.rpi.shuttletracker.data.remote.ShuttleRemoteDataSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * The real [ShuttleRepository]. The `observe*` functions just repeatedly call the matching
 * one-shot [ShuttleRemoteDataSource] function on a timer (`emit`, wait [pollMs], repeat) so
 * ViewModels can collect a live-updating [Flow][kotlinx.coroutines.flow.Flow] instead of polling
 * by hand.
 * */
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

        override fun observeAnnouncements(pollMs: Long) =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(remoteDataSource.getAnnouncements())
                    delay(pollMs)
                }
            }

        override suspend fun getRoutes() = remoteDataSource.getRoutes()

        override suspend fun getSchedule() = remoteDataSource.getSchedule()
    }
