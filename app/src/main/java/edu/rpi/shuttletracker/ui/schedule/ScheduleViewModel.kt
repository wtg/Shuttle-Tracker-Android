package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel
    @Inject
    constructor(
        private val apiRepository: ApiRepository,
    ) : ViewModel() {
        // represents the ui state of the view
        private val _scheduleUiState = MutableStateFlow(ScheduleUIState())
        val scheduleUiState: StateFlow<ScheduleUIState> = _scheduleUiState

        init {
            loadAll()
        }

        fun loadAll() {
            if (scheduleUiState.value.schedule.isEmpty()) {
                getSchedule()
            }
        }

        /**
         * sets all the errors to none
         * */
        fun clearErrors() {
            loadAll()
            _scheduleUiState.update {
                it.copy(
                    unknownError = null,
                    networkError = null,
                    serverError = null,
                )
            }
        }

        private fun getSchedule() {
            viewModelScope.launch {
                readApiResponse(apiRepository.getSchedule()) { response ->
                    _scheduleUiState.update {
                        it.copy(schedule = response.reversed())
                    }
                }
            }
        }

        /**
         * Reads the network response and maps it to correct place
         * */
        private fun <T> readApiResponse(
            response: NetworkResponse<T, ErrorResponse>,
            success: (body: T) -> Unit,
        ) {
            when (response) {
                is NetworkResponse.Success -> success(response.body)
                is NetworkResponse.ServerError ->
                    _scheduleUiState.update {
                        it.copy(serverError = response)
                    }

                is NetworkResponse.NetworkError ->
                    _scheduleUiState.update {
                        it.copy(networkError = response)
                    }

                is NetworkResponse.UnknownError ->
                    _scheduleUiState.update {
                        it.copy(unknownError = response)
                    }
            }
        }
    }

@Immutable
data class ScheduleUIState(
    val schedule: List<Schedule> = listOf(),
    val networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    val serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    val unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
)
