package edu.rpi.shuttletracker.data.remote

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.mapper.toModel
import edu.rpi.shuttletracker.data.remote.dto.ErrorResponse
import java.net.SocketTimeoutException
import javax.inject.Inject

class RemoteShuttleDataSource
    @Inject
    constructor(
        private val shuttleApi: ShuttleApi,
    ) {
        suspend fun getVehicleLocations() =
            shuttleApi.getVehicleLocations().toNetworkResult { locations ->
                locations.mapValues { it.value.toModel() }
            }

        suspend fun getVehicleEtas() =
            shuttleApi.getVehicleEtas().toNetworkResult { etas ->
                etas.mapValues { it.value.toModel() }
            }

        suspend fun getVehicleVelocities() =
            shuttleApi.getVehicleVelocities().toNetworkResult { velocities ->
                velocities.mapValues { it.value.toModel() }
            }

        suspend fun getRoutes() =
            shuttleApi.getRoutes().toNetworkResult { routes ->
                routes.mapValues { it.value.toModel() }
            }

        suspend fun getAnnouncements() =
            shuttleApi.getAnnouncements().toNetworkResult { announcements ->
                announcements.map { it.toModel() }
            }

        suspend fun getSchedule() = shuttleApi.getSchedule().toNetworkResult { it.toModel() }

        suspend fun getAggregatedSchedule() =
            shuttleApi.getAggregatedSchedule().toNetworkResult { schedules ->
                schedules.map { it.toModel() }
            }

        suspend fun sendRegistrationToken(token: String) =
            shuttleApi.sendRegistrationToken(token).toNetworkResult { Unit }
    }

private inline fun <T, R> NetworkResponse<T, ErrorResponse>.toNetworkResult(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResponse.Success -> NetworkResult.Success(transform(body))
        is NetworkResponse.ServerError ->
            NetworkResult.Failure(
                NetworkError.Http(code ?: -1, body?.reason, toString()),
            )
        is NetworkResponse.NetworkError ->
            NetworkResult.Failure(
                if (error is SocketTimeoutException) {
                    NetworkError.Timeout(error)
                } else {
                    NetworkError.NoConnection(error)
                },
            )
        is NetworkResponse.UnknownError -> NetworkResult.Failure(NetworkError.Unknown(error, toString()))
    }
