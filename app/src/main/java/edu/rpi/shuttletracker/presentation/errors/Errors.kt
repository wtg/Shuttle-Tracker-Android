package edu.rpi.shuttletracker.presentation.errors

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
            onSecondaryRequest = { ignoreErrorRequest() },
            onPrimaryRequest = { retryErrorRequest() },
            errorType = "Network error",
            icon = Icons.Default.WifiOff,
        )
    }

    if (serverError != null) {
        Error(
            error = serverError,
            onSecondaryRequest = { ignoreErrorRequest() },
            onPrimaryRequest = { retryErrorRequest() },
            errorType = "Server error",
            icon = Icons.Default.Dns,
        )
    }

    if (unknownError != null) {
        Error(
            error = unknownError,
            onSecondaryRequest = { ignoreErrorRequest() },
            onPrimaryRequest = { retryErrorRequest() },
            errorType = "Unknown error",
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
    errorType: String = "Error",
    errorBody: String = "",
    icon: ImageVector = Icons.Default.Error,
    primaryButtonText: String = "Retry",
    secondaryButtonText: String = "Ignore",
    showSecondaryButton: Boolean = true,
) {
    AlertDialog(
        icon = { Icon(icon, "Error") },
        title = { Text(text = errorType) },
        text = {
            Text(
                text = (
                    if (errorBody != "") {
                        errorBody + "\n\n"
                    } else {
                        ""
                    }
                    ) + error?.toString(),
            )
        },
        onDismissRequest = { onSecondaryRequest() },

        dismissButton = {
            if (showSecondaryButton) {
                Button(onClick = { onSecondaryRequest() }) {
                    Text(text = secondaryButtonText)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onPrimaryRequest() }) {
                Text(text = primaryButtonText)
            }
        },
    )
}
