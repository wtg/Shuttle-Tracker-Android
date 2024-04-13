package edu.rpi.shuttletracker.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupScreenViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        suspend fun getStartPage() = userPreferencesRepository.getSetupStartIndex()

        fun getAnalyticsEnabled() = userPreferencesRepository.getAllowAnalytics()

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

        fun updateAllowAnalytics() {
            viewModelScope.launch {
                userPreferencesRepository.saveAllowAnalytics(true)
            }
        }
    }
