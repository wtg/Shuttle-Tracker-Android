package edu.rpi.shuttletracker.ui.setup

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
class SetupScreenViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val setupUiState = combine(
        userPreferencesRepository.getPrivacyPolicyAccepted(),
        userPreferencesRepository.getAboutAccepted(),
    ) { privatePolicy, about ->
        return@combine SetupUiState(
            privacyPolicyAccepted = privatePolicy,
            aboutAccepted = about,
        )
    }.stateIn(
        scope = viewModelScope,
        SharingStarted.WhileSubscribed(),
        SetupUiState(),
    )

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
}

data class SetupUiState(
    val privacyPolicyAccepted: Boolean = false,
    val aboutAccepted: Boolean = false,
)
