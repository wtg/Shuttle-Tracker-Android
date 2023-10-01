package edu.rpi.shuttletracker.ui.maps

import android.location.Location
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val apiRepository: ShuttleTrackerRepository,
) : ViewModel() {

    // represents the ui state of the view
    private val _mapsUiState = MutableStateFlow(MapsUIState())
    val mapsUiState: StateFlow<MapsUIState> = _mapsUiState

    // shared flow of the running busses, this is to be subscribed to in UI
    lateinit var runningBusesState: SharedFlow<Unit>
        private set

    init {
        loadAll()
        loadRunningBuses()
    }

    /**
     * loads any vars in ui state that hasn't been loaded
     * THIS IGNORES THE RUNNING BUSES AS THIS SHOULD BE SUBSCRIBED TO FROM UI
     * */
    fun loadAll() {
        if (mapsUiState.value.stops.isEmpty()) {
            loadStops()
        }

        if (mapsUiState.value.routes.isEmpty()) {
            loadRoutes()
        }

        if (mapsUiState.value.allBuses.isEmpty()) {
            loadAllBuses()
        }
    }

    /**
     * @param location: users current location
     * @return returns the distance to closest stop in METERS
     * */
    fun closestDistanceToStop(location: Location): Float =
        _mapsUiState.value.stops.minOf {
            location.distanceTo(
                Location("stop").apply {
                    longitude = it.longitude
                    latitude = it.latitude
                },
            )
        }

    /**
     * sets all the errors to none
     * */
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

    /**
     * Creates a shared flow to update the ui state when subscribed
     * THIS MUST BE SUBSCRIBED TO IN UI
     * */
    private fun loadRunningBuses() {
        viewModelScope.launch {
            runningBusesState = apiRepository.getRunningBuses()
                .map { response ->
                    readApiResponse(response) { buses ->
                        _mapsUiState.update {
                            it.copy(runningBuses = buses)
                        }
                    }
                }
                .shareIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    1,
                )
        }
    }

    /**
     * Loads all possible buses and maps the API response
     * */
    private fun loadAllBuses() {
        viewModelScope.launch {
            readApiResponse(apiRepository.getAllBuses()) { buses ->
                _mapsUiState.update {
                    it.copy(allBuses = buses.sorted())
                }
            }
        }
    }

    /**
     * Loads all possible stops and maps the API response
     * */
    private fun loadStops() {
        viewModelScope.launch {
            readApiResponse(apiRepository.getStops()) { stops ->
                _mapsUiState.update {
                    it.copy(stops = stops)
                }
            }
        }
    }

    /**
     * Loads all possible routes and maps the API response
     * */
    private fun loadRoutes() {
        viewModelScope.launch {
            readApiResponse(apiRepository.getRoutes()) { routes ->
                _mapsUiState.update {
                    it.copy(routes = routes)
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
            is NetworkResponse.ServerError -> _mapsUiState.update { it.copy(serverError = response) }
            is NetworkResponse.NetworkError -> _mapsUiState.update { it.copy(networkError = response) }
            is NetworkResponse.UnknownError -> _mapsUiState.update { it.copy(unknownError = response) }
        }
    }
}

/**
 * Representation of the screen
 * */
data class MapsUIState(
    val runningBuses: List<Bus> = listOf(),
    val allBuses: List<Int> = listOf(),
    val stops: List<Stop> = listOf(),
    val routes: List<Route> = listOf(),
    val networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    val serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    val unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
)
