package edu.rpi.shuttletracker.core.network

/** A network call's data or its handled [NetworkError]. */
sealed interface NetworkResult<out T> {
    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>

    data class Failure(
        val error: NetworkError,
    ) : NetworkResult<Nothing>
}
