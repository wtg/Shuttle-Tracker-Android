package edu.rpi.shuttletracker.data.remote

import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.mapper.toModel
import edu.rpi.shuttletracker.data.remote.dto.ErrorResponse
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/** Calls [ShuttleApi] and converts responses or exceptions into [NetworkResult]. */
class RetrofitShuttleRemoteDataSource
    @Inject
    constructor(
        private val shuttleApi: ShuttleApi,
        retrofit: Retrofit,
    ) : ShuttleRemoteDataSource {
        private val errorConverter: Converter<ResponseBody, ErrorResponse> =
            retrofit.responseBodyConverter(ErrorResponse::class.java, emptyArray())

        override suspend fun getVehicleLocations() =
            execute(shuttleApi::getVehicleLocations) { locations ->
                locations.mapValues { it.value.toModel() }
            }

        override suspend fun getVehicleEtas() =
            execute(shuttleApi::getVehicleEtas) { etas ->
                etas.mapValues { it.value.toModel() }
            }

        override suspend fun getVehicleVelocities() =
            execute(shuttleApi::getVehicleVelocities) { velocities ->
                velocities.mapValues { it.value.toModel() }
            }

        override suspend fun getRoutes() =
            execute(shuttleApi::getRoutes) { routes ->
                routes.mapValues { it.value.toModel() }
            }

        override suspend fun getAnnouncements() =
            execute(shuttleApi::getAnnouncements) { response ->
                response.toModel()
            }

        override suspend fun getSchedule() = execute(shuttleApi::getSchedule) { it.toModel() }

        private suspend inline fun <T, R> execute(
            request: suspend () -> Response<T>,
            transform: (T) -> R,
        ): NetworkResult<R> =
            try {
                val response = request()
                val body = response.body()

                when {
                    response.isSuccessful && body != null -> NetworkResult.Success(transform(body))
                    response.isSuccessful ->
                        NetworkResult.Failure(
                            NetworkError.Unknown(
                                IllegalStateException("Successful response had no body"),
                            ),
                        )
                    else -> {
                        val errorBody =
                            response.errorBody()?.let { body ->
                                runCatching { errorConverter.convert(body) }.getOrNull()
                            }
                        NetworkResult.Failure(
                            NetworkError.Http(
                                statusCode = response.code(),
                                message = errorBody?.reason,
                                displayMessage = response.message(),
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SocketTimeoutException) {
                NetworkResult.Failure(NetworkError.Timeout(error))
            } catch (error: IOException) {
                NetworkResult.Failure(NetworkError.NoConnection(error))
            } catch (error: Exception) {
                NetworkResult.Failure(NetworkError.Unknown(error))
            }
    }
