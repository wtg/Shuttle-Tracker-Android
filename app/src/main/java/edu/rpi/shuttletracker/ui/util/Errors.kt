package edu.rpi.shuttletracker.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.ErrorResponse
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
    networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
    ignoreErrorRequest: () -> Unit = {},
    retryErrorRequest: () -> Unit = {},
) {
    if (networkError != null) {
        Error(
            error = networkError,
            onSecondaryRequest = { ignoreErrorRequest() },
            onPrimaryRequest = { retryErrorRequest() },
            errorType = stringResource(R.string.error_network),
            icon = Icons.Outlined.WifiOff,
            errorBody = networkError.error.toString()
        )
    }

    if (serverError != null) {
        Error(
            error = serverError,
            onSecondaryRequest = { ignoreErrorRequest() },
            onPrimaryRequest = { retryErrorRequest() },
            errorType = stringResource(R.string.error_server),
            icon = Icons.Outlined.Dns,
        )
    }

    if (unknownError != null) {
        Error(
            error = unknownError,
            onSecondaryRequest = { ignoreErrorRequest() },
            onPrimaryRequest = { retryErrorRequest() },
            errorType = stringResource(R.string.error_unknown),
        )
    }
}

/**
 * @param error: the error you want to display
 * @param onSecondaryRequest: what happens when error is ignored
 * @param onPrimaryRequest: what happens when you want to retry what caused the error
 *
 * @param errorType: What kind of error has occurred
 * @param icon: The icon for the alert
 * */
@Composable
fun Error(
    error: Any?,
    onSecondaryRequest: () -> Unit,
    onPrimaryRequest: () -> Unit,
    errorType: String = stringResource(R.string.error),
    errorBody: String = error?.toString() ?: "",
    icon: ImageVector = Icons.Outlined.Error,
    primaryButtonText: String = stringResource(R.string.retry),
    secondaryButtonText: String = stringResource(R.string.ignore),
    showSecondaryButton: Boolean = true,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SnackbarHost(hostState = snackbarHostState)
    LaunchedEffect(error) {
        if (error != null) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    "$errorType: $errorBody",
                    actionLabel = primaryButtonText
                )

                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        onPrimaryRequest()
                    }

                    SnackbarResult.Dismissed -> {
                        /* ignored */
                    }
                }
            }
        }
    }
}
