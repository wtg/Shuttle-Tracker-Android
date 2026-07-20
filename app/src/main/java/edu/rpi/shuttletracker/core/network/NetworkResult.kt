package edu.rpi.shuttletracker.core.network

sealed interface NetworkResult<out T> {
    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>

    data class Failure(
        val error: NetworkError,
    ) : NetworkResult<Nothing>
}
