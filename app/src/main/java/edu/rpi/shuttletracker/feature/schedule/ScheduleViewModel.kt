package edu.rpi.shuttletracker.feature.schedule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Route
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
 * Backs [ScheduleScreen]. Loads routes and the schedule once each (same load-if-missing pattern as
 * [edu.rpi.shuttletracker.feature.map.MapsViewModel]'s routes), and [refresh] drops both and
 * reloads on demand.
 * */
@HiltViewModel
class ScheduleViewModel
    @Inject
    constructor(
        private val shuttleRepository: ShuttleRepository,
    ) : ViewModel() {
        private val _scheduleUiState = MutableStateFlow(ScheduleUiState())
        val scheduleUiState: StateFlow<ScheduleUiState> = _scheduleUiState.asStateFlow()
        private var routesJob: Job? = null
        private var scheduleJob: Job? = null

        init {
            loadAll()
        }

        private fun loadAll() {
            if (scheduleUiState.value.routes.isEmpty()) loadRoutes()
            if (scheduleUiState.value.schedule == null) loadSchedule()
        }

        fun clearErrors() {
            _scheduleUiState.update {
                it.copy(
                    unknownError = null,
                    networkError = null,
                    serverError = null,
                )
            }
        }

        fun retry() {
            clearErrors()
            loadAll()
        }

        /**
         * Drops the cached routes and schedule, then fetches both again, so a stale cache never
         * survives an explicit user refresh. Not currently wired to any button in [ScheduleScreen] -
         * it's here (and tested) as the supported way to invalidate the cache when one is added.
         * */
        fun refresh() {
            routesJob?.cancel()
            routesJob = null
            scheduleJob?.cancel()
            scheduleJob = null
            _scheduleUiState.update { it.copy(routes = emptyMap(), schedule = null) }
            loadAll()
        }

        private fun loadRoutes() {
            if (routesJob?.isActive == true) return
            routesJob =
                viewModelScope.launch {
                    readApiResponse(shuttleRepository.getRoutes()) { routes ->
                        _scheduleUiState.update {
                            it.copy(routes = routes)
                        }
                    }
                }
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
                is NetworkResult.Failure ->
                    when (val error = response.error) {
                        is NetworkError.Connectivity ->
                            _scheduleUiState.update { it.copy(networkError = error) }
                        is NetworkError.Http ->
                            _scheduleUiState.update { it.copy(serverError = error) }
                        is NetworkError.Unknown ->
                            _scheduleUiState.update { it.copy(unknownError = error) }
                    }
            }
        }
    }

/** Everything the Schedule tab needs to render. See [ScheduleViewModel] for how it's filled in. */
@Immutable
data class ScheduleUiState(
    val schedule: Schedule? = null,
    val isScheduleLoading: Boolean = true,
    val routes: Map<String, Route> = emptyMap(),
    val networkError: NetworkError.Connectivity? = null,
    val serverError: NetworkError.Http? = null,
    val unknownError: NetworkError.Unknown? = null,
)
