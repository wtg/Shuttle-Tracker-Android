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
 * @param error a network error, null if none
 *
 * @param ignoreErrorRequest: what happens when error is ignored
 * @param retryErrorRequest: what happens when you want to retry what caused the error
 * */
@Composable
fun CheckResponseError(
    error: NetworkError? = null,
    ignoreErrorRequest: () -> Unit = {},
    retryErrorRequest: () -> Unit = {},
) {
    val networkMessage = stringResource(R.string.error_network)
    val serverMessage = stringResource(R.string.error_server)
    val unknownMessage = stringResource(R.string.error_unknown)
    val errorType =
        when (error) {
            is NetworkError.Connectivity -> networkMessage
            is NetworkError.Http -> serverMessage
            is NetworkError.Unknown -> unknownMessage
            null -> ""
        }
    val errorBody =
        when (error) {
            is NetworkError.NoConnection -> error.cause?.message.orEmpty()
            is NetworkError.Timeout -> error.cause?.message.orEmpty()
            is NetworkError.Http -> error.displayMessage
            is NetworkError.Unknown -> error.displayMessage
            null -> ""
        }

    Error(
        error = error,
        onPrimaryRequest = retryErrorRequest,
        onDismissRequest = ignoreErrorRequest,
        errorType = errorType,
        errorBody = errorBody,
    )
}

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
