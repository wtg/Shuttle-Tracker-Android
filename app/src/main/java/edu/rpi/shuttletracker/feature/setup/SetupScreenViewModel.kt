package edu.rpi.shuttletracker.feature.setup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [SetupScreen]. [completeCurrentPage] saves that page's acceptance (about/privacy policy)
 * to [UserPreferences] and advances to the next [SetupPage], or - on the last page - marks setup
 * complete and sets [SetupUiState.isComplete].
 * */
@HiltViewModel
class SetupScreenViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
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
                            userPreferences.saveAboutAccepted(true)
                            _uiState.update { it.copy(page = SetupPage.PrivacyPolicy) }
                        }

                        SetupPage.PrivacyPolicy -> {
                            userPreferences.savePrivacyPolicyAccepted(true)
                            _uiState.update { it.copy(page = SetupPage.Permissions) }
                        }

                        SetupPage.Permissions -> {
                            userPreferences.saveSetupCompleted(true)
                            _uiState.update { it.copy(isComplete = true) }
                        }
                    }
                }
        }
    }

/** Which [SetupPage] is showing, and whether the whole flow has finished. */
@Immutable
data class SetupUiState(
    val page: SetupPage = SetupPage.About,
    val isComplete: Boolean = false,
)
