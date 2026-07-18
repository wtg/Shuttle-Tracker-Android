package edu.rpi.shuttletracker.feature.settings.developerMenu

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.network.normalizeBaseUrl
import edu.rpi.shuttletracker.feature.settings.components.SettingsItem

@Composable
fun BaseUrlSettingItem(
    currentUrl: String,
    updateBaseUrl: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var textFieldUrl by remember { mutableStateOf(currentUrl) }
    val context = LocalContext.current
    val invalidUrlMessage = stringResource(R.string.invalid_url)

    LaunchedEffect(showDialog, currentUrl) {
        if (showDialog) textFieldUrl = currentUrl
    }

    SettingsItem(
        icon = Icons.Outlined.Link,
        title = stringResource(R.string.base_url),
        description = currentUrl,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(R.string.base_url)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.change_url_warning))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = textFieldUrl,
                        onValueChange = { textFieldUrl = it },
                        label = { Text(text = stringResource(R.string.url)) },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalizedUrl = normalizeBaseUrl(textFieldUrl)
                        if (normalizedUrl != null) {
                            updateBaseUrl(normalizedUrl)
                            showDialog = false
                        } else {
                            Toast.makeText(context, invalidUrlMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
