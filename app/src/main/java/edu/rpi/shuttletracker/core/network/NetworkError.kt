package edu.rpi.shuttletracker.core.network

sealed interface NetworkError {
    sealed interface Connectivity : NetworkError

    data class NoConnection(
        val cause: Throwable? = null,
    ) : Connectivity

    data class Timeout(
        val cause: Throwable? = null,
    ) : Connectivity

    data class Http(
        val statusCode: Int,
        val message: String? = null,
        val displayMessage: String = message.orEmpty(),
    ) : NetworkError

    data class Unknown(
        val cause: Throwable? = null,
        val displayMessage: String = cause.toString(),
    ) : NetworkError
}
