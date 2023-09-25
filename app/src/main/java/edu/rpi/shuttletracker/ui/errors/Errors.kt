package edu.rpi.shuttletracker.ui.errors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.ErrorResponse

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
            ignoreErrorRequest = { ignoreErrorRequest() },
            retryErrorRequest = { retryErrorRequest() },
            "Network error",
            Icons.Default.WifiOff,
        )
    }

    if (serverError != null) {
        Error(
            error = serverError,
            ignoreErrorRequest = { ignoreErrorRequest() },
            retryErrorRequest = { retryErrorRequest() },
            "Server error",
            Icons.Default.Dns,
        )
    }

    if (unknownError != null) {
        Error(
            error = unknownError,
            ignoreErrorRequest = { ignoreErrorRequest() },
            retryErrorRequest = { retryErrorRequest() },

            "Unknown error",
        )
    }
}

/**
 * @param error: the error you want to display
 * @param ignoreErrorRequest: what happens when error is ignored
 * @param retryErrorRequest: what happens when you want to retry what caused the error
 *
 * @param errorType: What kind of error has occurred
 * @param icon: The icon for the alert
 * */
@Composable
private fun Error(
    error: Any,
    ignoreErrorRequest: () -> Unit,
    retryErrorRequest: () -> Unit,
    errorType: String = "Error",
    icon: ImageVector = Icons.Default.Error,
) {
    AlertDialog(
        icon = { Icon(icon, "Error") },
        title = { Text(text = errorType) },
        text = { Text(text = error.toString()) },
        onDismissRequest = { ignoreErrorRequest() },
        dismissButton = {
            Button(onClick = { ignoreErrorRequest() }) {
                Text(text = "Ignore")
            }
        },
        confirmButton = {
            Button(onClick = { retryErrorRequest() }) {
                Text(text = "Retry")
            }
        },
    )
}
