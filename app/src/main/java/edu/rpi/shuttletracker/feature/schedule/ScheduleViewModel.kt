package edu.rpi.shuttletracker.feature.schedule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [ScheduleScreen]. Loads the schedule once and exposes retryable error state.
 * */
@HiltViewModel
class ScheduleViewModel
    @Inject
    constructor(
        private val shuttleRepository: ShuttleRepository,
    ) : ViewModel() {
        private val _scheduleUiState = MutableStateFlow(ScheduleUiState())
        val scheduleUiState: StateFlow<ScheduleUiState> = _scheduleUiState.asStateFlow()
        private var scheduleJob: Job? = null

        init {
            loadAll()
        }

        private fun loadAll() {
            if (scheduleUiState.value.schedule == null) loadSchedule()
        }

        fun clearErrors() {
            _scheduleUiState.update { it.copy(error = null) }
        }

        fun retry() {
            clearErrors()
            loadAll()
        }

        private fun loadSchedule() {
            if (scheduleJob?.isActive == true) return
            _scheduleUiState.update { it.copy(isScheduleLoading = true) }
            scheduleJob =
                viewModelScope.launch {
                    readApiResponse(shuttleRepository.getSchedule()) { schedule ->
                        _scheduleUiState.update {
                            it.copy(schedule = schedule, isScheduleLoading = false)
                        }
                    }
                    _scheduleUiState.update { it.copy(isScheduleLoading = false) }
                }
        }

        private fun <T> readApiResponse(
            response: NetworkResult<T>,
            success: (body: T) -> Unit,
        ) {
            when (response) {
                is NetworkResult.Success -> success(response.data)
                is NetworkResult.Failure -> _scheduleUiState.update { it.copy(error = response.error) }
            }
        }
    }

/** Everything the Schedule tab needs to render. See [ScheduleViewModel] for how it's filled in. */
@Immutable
data class ScheduleUiState(
    val schedule: Schedule? = null,
    val isScheduleLoading: Boolean = true,
    val error: NetworkError? = null,
)
