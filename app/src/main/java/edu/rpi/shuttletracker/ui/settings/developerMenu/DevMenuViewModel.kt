package edu.rpi.shuttletracker.ui.settings.developerMenu

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.DeparturesRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val apiRepository: ApiRepository,
        private val departuresRepository: DeparturesRepository,
    ) : ViewModel() {
        val devMenuUiState =
            combine(
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

        /**
         * MAKE SURE THIS IS BLOCKING OR ELSE STUFF BREAKS
         * */
        fun updateBaseUrl(baseUrl: String) {
            viewModelScope.launch {
                apiRepository.sendAnalytics(Event(serverBaseURL = baseUrl))
            }

            runBlocking {
                userPreferencesRepository.saveBaseUrl(baseUrl)
            }
        }

        /**
         * MAKE SURE THIS IS BLOCKING OR ELSE STUFF BREAKS
         * */
        fun updateAutoBoardServiceBlocking(autoBoardService: Boolean) {
            runBlocking {
                userPreferencesRepository.saveAutoBoardService(autoBoardService)
            }
        }

        fun updateDevMenu(devOptions: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.activateDevOptions(devOptions)
            }
        }

        fun restartAllDepartures(context: Context) {
            viewModelScope.launch {
                departuresRepository.getAllDepartures().first().forEach { it.initiateAlarms(context) }
            }
        }
    }

@Immutable
data class DevMenuUiState(
    val maxStopDist: Float = 20F,
    val baseUrl: String = "",
)
