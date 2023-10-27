package edu.rpi.shuttletracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val settingsUiState = combine(
        userPreferencesRepository.getAutoBoardService(),
        userPreferencesRepository.getColorBlindMode(),
        userPreferencesRepository.getDevOptions(),
    ) { autoBoardService, colorBindMode, devOptionState ->
        return@combine SettingsUiState(
            autoBoardService = autoBoardService,
            colorBlindMode = colorBindMode,
            devOptionState = devOptionState,
        )
    }.stateIn(
        scope = viewModelScope,
        SharingStarted.WhileSubscribed(),
        SettingsUiState(),
    )

    fun updateAutoBoardService(autoBoardService: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveAutoBoardService(autoBoardService)
        }
    }

    fun updateColorBlindMode(colorBlindMode: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveColorBlindMode(colorBlindMode)
        }
    }
}
data class SettingsUiState(
    val autoBoardService: Boolean = false,
    val colorBlindMode: Boolean = false,
    val devOptionState: Boolean = false,
)
