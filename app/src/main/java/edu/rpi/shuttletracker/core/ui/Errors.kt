package edu.rpi.shuttletracker.core.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.network.NetworkError

/**
 * Drop this in a `Scaffold`'s `snackbarHost` slot alongside a ViewModel's error state fields.
 * Shows a snackbar with a Retry action for whichever error is non-null (at most one is shown at a
 * time), and calls back so the ViewModel can clear or retry. See `MapsScreen`/`ScheduleScreen` for
 * how features wire this up.
 *
 * @param networkError: a network error, null if none
 * @param serverError: a server error, null if none
 * @param unknownError: an unknown error, null if none
 *
 * @param ignoreErrorRequest: what happens when error is ignored
 * @param retryErrorRequest: what happens when you want to retry what caused the error
 * */
@Composable
fun CheckResponseError(
    networkError: NetworkError.Connectivity? = null,
    serverError: NetworkError.Http? = null,
    unknownError: NetworkError.Unknown? = null,
    ignoreErrorRequest: () -> Unit = {},
    retryErrorRequest: () -> Unit = {},
) {
    val networkMessage = stringResource(R.string.error_network)
    val serverMessage = stringResource(R.string.error_server)
    val unknownMessage = stringResource(R.string.error_unknown)
    val activeError =
        when {
            networkError != null ->
                ErrorContent(
                    networkError,
                    networkMessage,
                    when (networkError) {
                        is NetworkError.NoConnection -> networkError.cause?.message.orEmpty()
                        is NetworkError.Timeout -> networkError.cause?.message.orEmpty()
                    },
                )
            serverError != null -> ErrorContent(serverError, serverMessage, serverError.displayMessage)
            unknownError != null -> ErrorContent(unknownError, unknownMessage, unknownError.displayMessage)
            else -> null
        }

    Error(
        error = activeError?.error,
        onPrimaryRequest = retryErrorRequest,
        onDismissRequest = ignoreErrorRequest,
        errorType = activeError?.type.orEmpty(),
        errorBody = activeError?.body.orEmpty(),
    )
}

private data class ErrorContent(
    val error: Any,
    val type: String,
    val body: String,
)

/**
 * @param error: the error you want to display
 * @param onPrimaryRequest: what happens when you want to retry what caused the error
 *
 * @param errorType: What kind of error has occurred
 * */
@Composable
fun Error(
    error: Any?,
    onPrimaryRequest: () -> Unit,
    onDismissRequest: () -> Unit = {},
    errorType: String = stringResource(R.string.error),
    errorBody: String = error?.toString() ?: "",
    primaryButtonText: String = stringResource(R.string.retry),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val currentPrimaryRequest = rememberUpdatedState(onPrimaryRequest)
    val currentDismissRequest = rememberUpdatedState(onDismissRequest)

    SnackbarHost(hostState = snackbarHostState)
    LaunchedEffect(error) {
        if (error != null) {
            val result =
                snackbarHostState.showSnackbar(
                    "$errorType: $errorBody",
                    actionLabel = primaryButtonText,
                    withDismissAction = true,
                )

            when (result) {
                SnackbarResult.ActionPerformed -> currentPrimaryRequest.value()
                SnackbarResult.Dismissed -> currentDismissRequest.value()
            }
        }
    }
}
