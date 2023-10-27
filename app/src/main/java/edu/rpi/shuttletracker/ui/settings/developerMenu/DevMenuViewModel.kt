package edu.rpi.shuttletracker.ui.settings.developerMenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val DevMenuUiState = combine(
        userPreferencesRepository.getMaxStopDist(),
        userPreferencesRepository.getBaseUrl(),
    ) { maxStopDist, baseUrl ->
        return@combine DevMenuUiState(
            maxStopDist = maxStopDist,
            baseUrl = baseUrl,
        )
    }.stateIn(
        scope = viewModelScope,
        SharingStarted.WhileSubscribed(),
        DevMenuUiState(),
    )
    fun updateMinStopDist(minStopDist: Float) {
        viewModelScope.launch {
            userPreferencesRepository.saveMaxStopDist(minStopDist)
        }
    }
    fun updateBaseUrl(baseUrl: String) {
        runBlocking {
            userPreferencesRepository.saveBaseUrl(baseUrl)
        }
    }
    fun updateAutoBoardServiceBlocking(autoBoardService: Boolean) {
        runBlocking {
            userPreferencesRepository.saveAutoBoardService(autoBoardService)
        }
    }
}

data class DevMenuUiState(
    val maxStopDist: Float = 20F,
    val baseUrl: String = "",
)
