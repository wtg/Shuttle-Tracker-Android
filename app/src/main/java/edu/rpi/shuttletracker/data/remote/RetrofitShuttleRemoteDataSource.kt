package edu.rpi.shuttletracker.data.remote

import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.mapper.toModel
import edu.rpi.shuttletracker.data.remote.dto.ErrorResponse
import kotlinx.coroutines.CancellationException
import java.net.SocketTimeoutException
import javax.inject.Inject

class RetrofitShuttleRemoteDataSource
    @Inject
    constructor(
        private val shuttleApi: ShuttleApi,
    ) : ShuttleRemoteDataSource {
        override suspend fun getVehicleLocations() =
            shuttleApi.getVehicleLocations().toNetworkResult { locations ->
                locations.mapValues { it.value.toModel() }
            }

        override suspend fun getVehicleEtas() =
            shuttleApi.getVehicleEtas().toNetworkResult { etas ->
                etas.mapValues { it.value.toModel() }
            }

        override suspend fun getVehicleVelocities() =
            shuttleApi.getVehicleVelocities().toNetworkResult { velocities ->
                velocities.mapValues { it.value.toModel() }
            }

        override suspend fun getRoutes() =
            shuttleApi.getRoutes().toNetworkResult { routes ->
                routes.mapValues { it.value.toModel() }
            }

        override suspend fun getAnnouncements() =
            shuttleApi.getAnnouncements().toNetworkResult { announcements ->
                announcements.map { it.toModel() }
            }

        override suspend fun getSchedule() = shuttleApi.getSchedule().toNetworkResult { it.toModel() }

        override suspend fun sendRegistrationToken(token: String) =
            shuttleApi.sendRegistrationToken(token).toNetworkResult { Unit }
    }

private inline fun <T, R> NetworkResponse<T, ErrorResponse>.toNetworkResult(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResponse.Success ->
            try {
                NetworkResult.Success(transform(body))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                NetworkResult.Failure(NetworkError.Unknown(error))
            }
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
