package edu.rpi.shuttletracker.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val apiRepository: ShuttleTrackerRepository,
) : ViewModel() {
    private val _mapsUiState = MutableStateFlow(MapsUIState())
    val mapsUIState: StateFlow<MapsUIState> = _mapsUiState

    init {
        loadAll()
    }

    fun loadAll() {
        if (mapsUIState.value.stops.isEmpty()) {
            loadStops()
        }

        if (mapsUIState.value.routes.isEmpty()) {
            loadRoutes()
        }

        if (mapsUIState.value.allBuses.isEmpty()) {
            loadAllBuses()
        }

        loadRunningBuses()
    }

    fun clearErrors() {
        loadAll()
        _mapsUiState.update {
            it.copy(
                unknownError = null,
                networkError = null,
                serverError = null,
            )
        }
    }

    private fun loadRunningBuses() {
        viewModelScope.launch {
            when (val response = apiRepository.getRunningBuses()) {
                is NetworkResponse.Success -> updateRunningBuses(response.body)
                is NetworkResponse.ServerError -> serverError(response)
                is NetworkResponse.NetworkError -> networkError(response)
                is NetworkResponse.UnknownError -> unknownError(response)
            }
        }
    }

    private fun loadAllBuses() {
        viewModelScope.launch {
            when (val response = apiRepository.getAllBuses()) {
                is NetworkResponse.Success -> updateAllBuses(response.body)
                is NetworkResponse.ServerError -> serverError(response)
                is NetworkResponse.NetworkError -> networkError(response)
                is NetworkResponse.UnknownError -> unknownError(response)
            }
        }
    }

    private fun loadStops() {
        viewModelScope.launch {
            when (val response = apiRepository.getStops()) {
                is NetworkResponse.Success -> updateStops(response.body)
                is NetworkResponse.ServerError -> serverError(response)
                is NetworkResponse.NetworkError -> networkError(response)
                is NetworkResponse.UnknownError -> unknownError(response)
            }
        }
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            when (val response = apiRepository.getRoutes()) {
                is NetworkResponse.Success -> updateRoutes(response.body)
                is NetworkResponse.ServerError -> serverError(response)
                is NetworkResponse.NetworkError -> networkError(response)
                is NetworkResponse.UnknownError -> unknownError(response)
            }
        }
    }

    private fun networkError(error: NetworkResponse.NetworkError<*, ErrorResponse>) {
        _mapsUiState.update {
            it.copy(networkError = error)
        }
    }

    private fun serverError(error: NetworkResponse.ServerError<*, ErrorResponse>) {
        _mapsUiState.update {
            it.copy(serverError = error)
        }
    }
    private fun unknownError(error: NetworkResponse.UnknownError<*, ErrorResponse>) {
        _mapsUiState.update {
            it.copy(unknownError = error)
        }
    }

    private fun updateRunningBuses(buses: List<Bus>) {
        _mapsUiState.update {
            it.copy(runningBuses = buses)
        }
    }

    private fun updateAllBuses(buses: List<Int>) {
        _mapsUiState.update {
            it.copy(allBuses = buses)
        }
    }

    private fun updateStops(stops: List<Stop>) {
        _mapsUiState.update {
            it.copy(stops = stops)
        }
    }

    private fun updateRoutes(routes: List<Route>) {
        _mapsUiState.update {
            it.copy(routes = routes)
        }
    }
}

data class MapsUIState(
    val runningBuses: List<Bus> = listOf(),
    val allBuses: List<Int> = listOf(),
    val stops: List<Stop> = listOf(),
    val routes: List<Route> = listOf(),
    val networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    val serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    val unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
)
