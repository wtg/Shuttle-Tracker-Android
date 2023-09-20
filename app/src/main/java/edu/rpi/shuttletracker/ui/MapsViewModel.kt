package edu.rpi.shuttletracker.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val apiRepository: ShuttleTrackerRepository,
) : ViewModel() {

    private val _mapsUiState = MutableStateFlow(MapsUIState(listOf()))
    val mapsUIState: StateFlow<MapsUIState> = _mapsUiState

    fun loadBuses() {
        viewModelScope.launch {
            Log.d("oncreate", "loadBuses: ${apiRepository.getBuses()}")
            _mapsUiState.value = _mapsUiState.value.copy(buses = apiRepository.getBuses())
        }
    }

    fun loadStops() {
        viewModelScope.launch {
            Log.d("oncreate", "loadStops: ${apiRepository.getStops()}")
            _mapsUiState.value = _mapsUiState.value.copy(stops = apiRepository.getStops())
        }
    }
}

data class MapsUIState(
    val buses: List<Bus> = listOf(),
    val stops: List<Stop> = listOf(),
)
