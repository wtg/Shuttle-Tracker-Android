package edu.rpi.shuttletracker.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        val settingsUiState =
            combine(
                userPreferencesRepository.getDevOptions(),
                userPreferencesRepository.getThemeMode(),
                userPreferencesRepository.getShuttleAnimations(),
                userPreferencesRepository.getShuttleRotation(),
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
                userPreferencesRepository.saveThemeMode(themeMode)
            }
        }

        fun updateShuttleAnimations(enabled: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.saveShuttleAnimations(enabled)
            }
        }

        fun updateShuttleRotation(enabled: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.saveShuttleRotations(enabled)
            }
        }

        suspend fun resetPreferences() {
            userPreferencesRepository.clearAllPreferences()
        }
    }

@Immutable
data class SettingsUiState(
    val devOptionState: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val animationsEnabled: Boolean = false,
    val rotationEnabled: Boolean = true,
)
