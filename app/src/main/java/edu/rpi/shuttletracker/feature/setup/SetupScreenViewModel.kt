package edu.rpi.shuttletracker.feature.setup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupScreenViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SetupUiState())
        val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

        private var transitionJob: Job? = null

        fun goToPreviousPage() {
            if (transitionJob?.isActive == true) return

            _uiState.update { state ->
                state.copy(page = state.page.previous())
            }
        }

        fun completeCurrentPage() {
            if (transitionJob?.isActive == true) return

            transitionJob =
                viewModelScope.launch {
                    when (uiState.value.page) {
                        SetupPage.About -> {
                            userPreferencesRepository.saveAboutAccepted(true)
                            _uiState.update { it.copy(page = SetupPage.PrivacyPolicy) }
                        }

                        SetupPage.PrivacyPolicy -> {
                            userPreferencesRepository.savePrivacyPolicyAccepted(true)
                            _uiState.update { it.copy(page = SetupPage.Permissions) }
                        }

                        SetupPage.Permissions -> {
                            userPreferencesRepository.saveSetupCompleted(true)
                            _uiState.update { it.copy(isComplete = true) }
                        }
                    }
                }
        }

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

@Immutable
data class SetupUiState(
    val page: SetupPage = SetupPage.About,
    val isComplete: Boolean = false,
)
