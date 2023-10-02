package edu.rpi.shuttletracker.ui.settings.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.util.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun AboutScreen(
    navigator: DestinationsNavigator,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "About") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 10.dp),
            ) {
                Text(text = stringResource(id = R.string.about_page))
            }

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(icon = Icons.Default.Code, title = "Check out the repository") {
                uriHandler.openUri("https://github.com/wtg/Shuttle-Tracker-Android")
            }

            SettingsItem(icon = Icons.Default.BugReport, title = "Report a problem") {
                uriHandler.openUri("https://github.com/wtg/Shuttle-Tracker-Android/issues")
            }

            SettingsItem(icon = Icons.Default.Info, title = "Version", BuildConfig.VERSION_NAME)
        }
    }
}
