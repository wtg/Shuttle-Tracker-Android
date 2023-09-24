package edu.rpi.shuttletracker.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
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
        // loads all of the variables on first load
        loadStops()
        loadRunningBuses()
        loadRoutes()
        loadAllBuses()
    }

    fun loadRunningBuses() {
        viewModelScope.launch {
            _mapsUiState.update {
                it.copy(runningBuses = apiRepository.getRunningBuses())
            }
        }
    }

    fun loadAllBuses() {
        viewModelScope.launch {
            _mapsUiState.update {
                it.copy(allBuses = apiRepository.getAllBuses())
            }
        }
    }

    fun loadStops() {
        viewModelScope.launch {
            _mapsUiState.update {
                it.copy(stops = apiRepository.getStops())
            }
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _mapsUiState.update {
                it.copy(routes = apiRepository.getRoutes())
            }
        }
    }

    fun addBus(busNum: Int, bus: BoardBus) {
        viewModelScope.launch {
            apiRepository.addBus(busNum, bus)
        }
    }
}

data class MapsUIState(
    val runningBuses: List<Bus> = listOf(),
    val allBuses: List<Int> = listOf(),
    val stops: List<Stop> = listOf(),
    val routes: List<Route> = listOf(),
)
