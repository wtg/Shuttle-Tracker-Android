package edu.rpi.shuttletracker.feature.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [AboutScreen]. Just the one action - everything else on that screen is static/links. */
@HiltViewModel
class AboutViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        fun activateDevOptions() {
            viewModelScope.launch {
                userPreferences.activateDevOptions(true)
            }
        }
    }
