package edu.rpi.shuttletracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DevMenuScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SetupScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.maps.MapsViewModel
import edu.rpi.shuttletracker.ui.theme.ThemeMode
import edu.rpi.shuttletracker.ui.util.SettingsItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
    mapsViewModel: MapsViewModel = hiltViewModel(),
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )

    val settingsUiState = viewModel.settingsUiState.collectAsStateWithLifecycle().value

    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    val mapsUiState = mapsViewModel.mapsUiState.collectAsStateWithLifecycle().value

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
        ) {
//            item {
//                ColorBlindSettingItem(
//                    colorBlindMode = settingsUiState.colorBlindMode,
//                    updateColorBlindMode = viewModel::updateColorBlindMode,
//                )
//            }

            item {
                ThemeModeSettingItem(
                    themeMode = settingsUiState.themeMode,
                    updateThemeMode = viewModel::updateThemeMode,
                )
            }
            if (settingsUiState.devOptionState) {
                item {
                    SettingsItem(
                        Icons.Outlined.Code,
                        stringResource(R.string.dev_options),
                        onClick = { navigator.navigate(DevMenuScreenDestination()) },
                    )
                }
            }

            item {
                SettingsItem(
                    Icons.Outlined.RestartAlt,
                    stringResource(R.string.redo_setup),
                    onClick = {
                        scope.launch {
                            viewModel.clearAllPreferences()
                            navigator.navigate(SetupScreenDestination())
                        }
                    },
                )
            }

            item {
                SettingsItem(
                    Icons.Outlined.Info,
                    stringResource(R.string.about),
                    onClick = { navigator.navigate(AboutScreenDestination()) },
                )
            }

            item {
                ShuttleAnimationSettingItem(
                    animationsEnabled = mapsUiState.shuttleAnimationsEnabled,
                    updateAnimations = mapsViewModel::setShuttleAnimations,
                    )
            }
            
        }
    }
}

@Composable
fun ColorBlindSettingItem(
    colorBlindMode: Boolean,
    updateColorBlindMode: (Boolean) -> Unit,
) {
    SettingsItem(
        icon = Icons.Outlined.Visibility,
        stringResource(R.string.color_blind_mode),
        stringResource(R.string.color_blind_description),
    ) {
        Switch(
            checked = colorBlindMode,
            onCheckedChange = { updateColorBlindMode(it) },
        )
    }
}

@Composable
fun ThemeModeSettingItem(
    themeMode: ThemeMode,
    updateThemeMode: (ThemeMode) -> Unit,
) {
    SettingsItem(
        icon = Icons.Outlined.Contrast,
        stringResource(R.string.app_theme),
        hasBottomSpacing = false,
    )

    val themeOptions = listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark)

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

@Composable
fun ShuttleAnimationSettingItem(
    animationsEnabled: Boolean,
    updateAnimations: (Boolean) -> Unit,) {
    SettingsItem(
        icon = Icons.Outlined.Contrast,
        title = "Shuttle Animations",)
    {
        Switch(
            checked = animationsEnabled,
            onCheckedChange = {updateAnimations(it)},
        )
    }

}