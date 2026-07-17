package edu.rpi.shuttletracker.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.repository.UserPreferencesRepository
import edu.rpi.shuttletracker.ui.theme.ThemeMode
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
                userPreferencesRepository.getColorBlindMode(),
                userPreferencesRepository.getDevOptions(),
                userPreferencesRepository.getThemeMode(),
                userPreferencesRepository.getShuttleAnimations(),
                userPreferencesRepository.getShuttleRotation(),
            ) { colorBindMode, devOptionState, themeMode, animationsEnabled, rotationEnabled ->
                return@combine SettingsUiState(
                    colorBlindMode = colorBindMode,
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

        fun updateColorBlindMode(colorBlindMode: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.saveColorBlindMode(colorBlindMode)
            }
        }

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

        fun clearAllPreferences() =
            viewModelScope.launch {
                userPreferencesRepository.clearAllPreferences()
            }
    }

@Immutable
data class SettingsUiState(
    val colorBlindMode: Boolean = false,
    val devOptionState: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val animationsEnabled: Boolean = false,
    val rotationEnabled: Boolean = true,
)
