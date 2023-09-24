package edu.rpi.shuttletracker.ui.errors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.ErrorResponse

@Composable
fun ServerError(
    error: NetworkResponse.ServerError<*, ErrorResponse>,
    onDismissRequest: () -> Unit,
    onSuccessRequest: () -> Unit,
) {
    AlertDialog(
        icon = { Icon(Icons.Default.Dns, "Server error") },
        title = { Text(text = "Server error") },
        text = { Text(text = error.toString()) },
        onDismissRequest = { onDismissRequest() },
        dismissButton = {
            Button(onClick = { onDismissRequest() }) {
                Text(text = "Ignore")
            }
        },
        confirmButton = {
            Button(onClick = { onSuccessRequest() }) {
                Text(text = "Retry")
            }
        },
    )
}
