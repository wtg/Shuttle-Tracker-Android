package edu.rpi.shuttletracker.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DevMenuScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MapsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SetupScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<RootGraph>
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.settingsUiState.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()

    SettingsContent(
        uiState = uiState,
        onBack = navigator::popBackStack,
        onThemeModeChange = viewModel::updateThemeMode,
        onRotationChange = viewModel::updateShuttleRotation,
        onAnimationsChange = viewModel::updateShuttleAnimations,
        onRedoSetup = {
            scope.launch {
                viewModel.resetSetup()
                navigator.navigate(SetupScreenDestination()) {
                    popUpTo(MapsScreenDestination) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        },
        onAbout = { navigator.navigate(AboutScreenDestination()) },
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                },
            )
        },
        onDeveloperOptions = { navigator.navigate(DevMenuScreenDestination()) },
    )
}
