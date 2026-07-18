package edu.rpi.shuttletracker.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.feature.settings.components.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRotationChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onRedoSetup: () -> Unit,
    onAbout: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDeveloperOptions: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                ThemeModeSettingItem(uiState.themeMode, onThemeModeChange)
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Explore,
                    title = stringResource(R.string.rotation),
                    description = stringResource(R.string.rotation_description),
                ) {
                    Switch(uiState.rotationEnabled, onCheckedChange = onRotationChange)
                }
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.DirectionsBus,
                    title = stringResource(R.string.animation),
                    description = stringResource(R.string.animation_description),
                ) {
                    Switch(uiState.animationsEnabled, onCheckedChange = onAnimationsChange)
                }
            }
            item {
                SettingsItem(
                    Icons.Outlined.RestartAlt,
                    stringResource(R.string.redo_setup),
                    onClick = onRedoSetup,
                )
            }
            item {
                SettingsItem(
                    Icons.Outlined.Info,
                    stringResource(R.string.about),
                    onClick = onAbout,
                )
            }
            item {
                SettingsItem(
                    Icons.Outlined.Settings,
                    stringResource(R.string.open_app_settings),
                    onClick = onOpenAppSettings,
                )
            }
            if (uiState.devOptionState) {
                item {
                    SettingsItem(
                        Icons.Outlined.Code,
                        stringResource(R.string.dev_options),
                        onClick = onDeveloperOptions,
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeModeSettingItem(
    themeMode: ThemeMode,
    updateThemeMode: (ThemeMode) -> Unit,
) {
    SettingsItem(
        icon = Icons.Outlined.Contrast,
        title = stringResource(R.string.app_theme),
        hasBottomSpacing = false,
    )

    val themeOptions = ThemeMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        SingleChoiceSegmentedButtonRow {
            themeOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                    onClick = { updateThemeMode(option) },
                    selected = themeMode == option,
                ) {
                    Text(option.name)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        SettingsContent(
            uiState = SettingsUiState(devOptionState = true),
            onBack = {},
            onThemeModeChange = {},
            onRotationChange = {},
            onAnimationsChange = {},
            onRedoSetup = {},
            onAbout = {},
            onOpenAppSettings = {},
            onDeveloperOptions = {},
        )
    }
}
