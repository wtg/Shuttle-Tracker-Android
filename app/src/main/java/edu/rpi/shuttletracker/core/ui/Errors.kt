package edu.rpi.shuttletracker.core.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.network.NetworkError
import kotlinx.coroutines.launch

/**
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
    if (networkError != null) {
        Error(
            error = networkError,
            onPrimaryRequest = { retryErrorRequest() },
            errorType = stringResource(R.string.error_network),
            errorBody =
                when (networkError) {
                    is NetworkError.NoConnection -> networkError.cause.toString()
                    is NetworkError.Timeout -> networkError.cause.toString()
                },
        )
    }

    if (serverError != null) {
        Error(
            error = serverError,
            onPrimaryRequest = { retryErrorRequest() },
            errorType = stringResource(R.string.error_server),
            errorBody = serverError.displayMessage,
        )
    }

    if (unknownError != null) {
        Error(
            error = unknownError,
            onPrimaryRequest = { retryErrorRequest() },
            errorType = stringResource(R.string.error_unknown),
            errorBody = unknownError.displayMessage,
        )
    }
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
    errorType: String = stringResource(R.string.error),
    errorBody: String = error?.toString() ?: "",
    primaryButtonText: String = stringResource(R.string.retry),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SnackbarHost(hostState = snackbarHostState)
    LaunchedEffect(error) {
        if (error != null) {
            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        "$errorType: $errorBody",
                        actionLabel = primaryButtonText,
                    )

                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        onPrimaryRequest()
                    }

                    SnackbarResult.Dismissed -> {
                        // ignored
                    }
                }
            }
        }
    }
}
