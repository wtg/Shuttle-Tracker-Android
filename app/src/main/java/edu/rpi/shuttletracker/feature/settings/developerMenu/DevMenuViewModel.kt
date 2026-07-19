package edu.rpi.shuttletracker.feature.settings.developerMenu

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        suspend fun setDeveloperOptions(enabled: Boolean) {
            userPreferences.activateDevOptions(enabled)
        }
    }
