package edu.rpi.shuttletracker.core.network

/**
 * The outcome of any network call the app makes: either [Success] with the data, or [Failure]
 * with a [NetworkError]. Every `ShuttleRepository`/`ShuttleRemoteDataSource` function returns one
 * of these instead of throwing, so callers handle failure as a normal value.
 * */
sealed interface NetworkResult<out T> {
    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>

    data class Failure(
        val error: NetworkError,
    ) : NetworkResult<Nothing>
}
