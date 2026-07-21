package edu.rpi.shuttletracker.core.network

/**
 * Every way a [ShuttleRepository][edu.rpi.shuttletracker.data.repository.ShuttleRepository] call
 * can fail, carried inside a [NetworkResult.Failure] instead of being thrown, so a ViewModel can
 * just `when` over it and update UI state - no try/catch needed at the call site.
 * */
sealed interface NetworkError {
    /** No connection or the request timed out - retrying later will likely work. */
    sealed interface Connectivity : NetworkError

    data class NoConnection(
        val cause: Throwable? = null,
    ) : Connectivity

    data class Timeout(
        val cause: Throwable? = null,
    ) : Connectivity

    /** The server responded, but with an error status code (4xx/5xx). */
    data class Http(
        val statusCode: Int,
        val message: String? = null,
        val displayMessage: String = message.orEmpty(),
    ) : NetworkError

    /** Anything else - an unexpected exception while making or parsing the request. */
    data class Unknown(
        val cause: Throwable? = null,
        val displayMessage: String = cause.toString(),
    ) : NetworkError
}
