package edu.rpi.shuttletracker.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupScreenViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        fun updatePrivacyPolicyAccepted() {
            viewModelScope.launch {
                userPreferencesRepository.savePrivacyPolicyAccepted(true)
            }
        }

        fun updateAboutAccepted() {
            viewModelScope.launch {
                userPreferencesRepository.saveAboutAccepted(true)
            }
        }

        fun completeSetup() {
            viewModelScope.launch {
                userPreferencesRepository.saveSetupCompleted(true)
            }
        }
    }
