package edu.rpi.shuttletracker.feature.settings.developerMenu

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.normalizeBaseUrl
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        val devMenuUiState =
            userPreferences
                .getBaseUrl()
                .map(::DevMenuUiState)
                .stateIn(
                    scope = viewModelScope,
                    SharingStarted.WhileSubscribed(),
                    DevMenuUiState(),
                )

        suspend fun saveBaseUrl(baseUrl: String): Boolean {
            val normalizedUrl = normalizeBaseUrl(baseUrl) ?: return false
            userPreferences.saveBaseUrl(normalizedUrl)
            return true
        }

        suspend fun setDeveloperOptions(enabled: Boolean) {
            userPreferences.activateDevOptions(enabled)
        }
    }

@Immutable
data class DevMenuUiState(
    val baseUrl: String = "",
)
