package edu.rpi.shuttletracker.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRedoSetup: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.settingsUiState.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()

    SettingsContent(
        uiState = uiState,
        onBack = onBack,
        onThemeModeChange = viewModel::updateThemeMode,
        onRotationChange = viewModel::updateShuttleRotation,
        onAnimationsChange = viewModel::updateShuttleAnimations,
        onRedoSetup = {
            scope.launch {
                viewModel.resetSetup()
                onRedoSetup()
            }
        },
        onAbout = onOpenAbout,
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                },
            )
        },
        onDeveloperOptions = onOpenDeveloperOptions,
    )
}
