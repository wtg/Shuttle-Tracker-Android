package edu.rpi.shuttletracker.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exposes settings directly from [UserPreferences] and writes updates back to it. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        val settingsUiState =
            combine(
                userPreferences.getDevOptions(),
                userPreferences.getThemeMode(),
                userPreferences.getShuttleAnimations(),
                userPreferences.getShuttleRotation(),
            ) { devOptionState, themeMode, animationsEnabled, rotationEnabled ->
                return@combine SettingsUiState(
                    devOptionState = devOptionState,
                    themeMode = themeMode,
                    animationsEnabled = animationsEnabled,
                    rotationEnabled = rotationEnabled,
                )
            }.stateIn(
                scope = viewModelScope,
                SharingStarted.WhileSubscribed(),
                SettingsUiState(),
            )

        fun updateThemeMode(themeMode: ThemeMode) {
            viewModelScope.launch {
                userPreferences.saveThemeMode(themeMode)
            }
        }

        fun updateShuttleAnimations(enabled: Boolean) {
            viewModelScope.launch {
                userPreferences.saveShuttleAnimations(enabled)
            }
        }

        fun updateShuttleRotation(enabled: Boolean) {
            viewModelScope.launch {
                userPreferences.saveShuttleRotations(enabled)
            }
        }

        suspend fun resetSetup() {
            userPreferences.resetSetup()
        }
    }

/** A direct mirror of the settings-related [UserPreferences] values. */
@Immutable
data class SettingsUiState(
    val devOptionState: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val animationsEnabled: Boolean = false,
    val rotationEnabled: Boolean = true,
)
