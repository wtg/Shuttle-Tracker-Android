package edu.rpi.shuttletracker.ui.maps

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.EmptyEvent
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel
    @Inject
    constructor(
        private val apiRepository: ApiRepository,
        userPreferencesRepository: UserPreferencesRepository,
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

            // sets auto board service state
            userPreferencesRepository.getAutoBoardService()
                .flowOn(Dispatchers.Default)
                .onEach { autoBoardService ->
                    _mapsUiState.update {
                        it.copy(autoBoardService = autoBoardService)
                    }
                }.launchIn(viewModelScope)

            // sets the notification read count
            userPreferencesRepository.getNotificationsRead()
                .flowOn(Dispatchers.Default)
                .onEach { count ->
                    _mapsUiState.update {
                        it.copy(notificationsRead = count)
                    }
                }.launchIn(viewModelScope)

            // gets user preference for colorblind mode
            userPreferencesRepository.getColorBlindMode()
                .flowOn(Dispatchers.Default)
                .onEach { colorBlindMode ->
                    _mapsUiState.update {
                        it.copy(colorBlindMode = colorBlindMode)
                    }
                }.launchIn(viewModelScope)

            userPreferencesRepository.getMaxStopDist()
                .flowOn(Dispatchers.Default)
                .onEach { minStopDist ->
                    _mapsUiState.update {
                        it.copy(minStopDist = minStopDist)
                    }
                }.launchIn(viewModelScope)
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

            if (mapsUiState.value.notificationsRead == -1) {
                loadAnnouncementCount()
            }
        }

        fun refreshRunningBusses() {
            viewModelScope.launch {
                readApiResponse(apiRepository.getRunningBuses().first()) { runningBusses ->
                    _mapsUiState.update {
                        it.copy(runningBuses = runningBusses)
                    }
                }
            }
        }

        /**
         * @param location: users current location
         * @return returns the distance to closest stop in METERS
         * */
        fun closestDistanceToStop(location: Location): Float =
            _mapsUiState.value.stops.minOfOrNull {
                location.distanceTo(
                    Location("stop").apply {
                        longitude = it.longitude
                        latitude = it.latitude
                    },
                )
            } ?: Float.MAX_VALUE

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

        private fun loadAnnouncementCount() {
            viewModelScope.launch {
                readApiResponse(apiRepository.getAnnouncements()) { announcements ->
                    _mapsUiState.update {
                        it.copy(totalAnnouncements = announcements.size)
                    }
                }
            }
        }

        /**
         * Creates a shared flow to update the ui state when subscribed
         * THIS MUST BE SUBSCRIBED TO IN UI
         * */
        private fun loadRunningBuses() {
            viewModelScope.launch {
                runningBusesState =
                    apiRepository.getRunningBuses()
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
                is NetworkResponse.ServerError ->
                    _mapsUiState.update {
                        it.copy(serverError = response)
                    }

                is NetworkResponse.NetworkError ->
                    _mapsUiState.update {
                        it.copy(networkError = response)
                    }

                is NetworkResponse.UnknownError ->
                    _mapsUiState.update {
                        it.copy(unknownError = response)
                    }
            }
        }

        fun leaveBusPressed() {
            viewModelScope.launch {
                apiRepository.sendAnalytics(Event(boardBusDeactivatedManual = true))
                apiRepository.sendAnalytics(Event(leaveBusTapped = EmptyEvent))
            }
        }

        fun boardBusPressed() {
            viewModelScope.launch {
                apiRepository.sendAnalytics(Event(boardBusTapped = EmptyEvent))
            }
        }

        fun busSelectionCanceled() {
            viewModelScope.launch {
                apiRepository.sendAnalytics(Event(busSelectionCanceled = EmptyEvent))
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
    val notificationsRead: Int = -1,
    val totalAnnouncements: Int = -1,
    val autoBoardService: Boolean = false,
    val colorBlindMode: Boolean = false,
    val minStopDist: Float = 50f,
)
