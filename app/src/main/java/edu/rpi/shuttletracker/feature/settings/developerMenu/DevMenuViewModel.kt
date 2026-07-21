package edu.rpi.shuttletracker.feature.settings.developerMenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [DevMenuScreen]. Same "mirror preferences directly" pattern as `SettingsViewModel`. */
@HiltViewModel
class DevMenuViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        val simulateAnnouncements: StateFlow<Boolean> =
            userPreferences
                .getSimulateAnnouncements()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        val fakeShuttlesEnabled: StateFlow<Boolean> =
            userPreferences
                .getFakeShuttlesEnabled()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        suspend fun setDeveloperOptions(enabled: Boolean) {
            userPreferences.activateDevOptions(enabled)
        }

        fun setSimulateAnnouncements(enabled: Boolean) {
            viewModelScope.launch {
                userPreferences.saveSimulateAnnouncements(enabled)
            }
        }

        fun setFakeShuttlesEnabled(enabled: Boolean) {
            viewModelScope.launch {
                userPreferences.saveFakeShuttlesEnabled(enabled)
            }
        }
    }
