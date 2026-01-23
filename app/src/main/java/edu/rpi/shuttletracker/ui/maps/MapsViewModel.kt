package edu.rpi.shuttletracker.ui.maps

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.MapType
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        // represents the ui state of the view
        private val _mapsUiState = MutableStateFlow(MapsUiState())
        val mapsUiState: StateFlow<MapsUiState> = _mapsUiState

        // shared flow of the running buses, this is to be subscribed to in UI
        lateinit var busesState: SharedFlow<Unit>
            private set

        init {
            loadAll()
            loadBuses()

            // gets user preference for colorblind mode
            userPreferencesRepository
                .getColorBlindMode()
                .flowOn(Dispatchers.Default)
                .onEach { colorBlindMode ->
                    _mapsUiState.update {
                        it.copy(colorBlindMode = colorBlindMode)
                    }
                }.launchIn(viewModelScope)

            // gets user preference for dark mode
            userPreferencesRepository
                .getThemeMode()
                .flowOn(Dispatchers.Default)
                .onEach { themeMode ->
                    _mapsUiState.update {
                        it.copy(themeMode = themeMode)
                    }
                }.launchIn(viewModelScope)

            userPreferencesRepository
                .getMapType()
                .flowOn(Dispatchers.Default)
                .onEach { mapType ->
                    _mapsUiState.update {
                        it.copy(mapType = mapType)
                    }
                }.launchIn(viewModelScope)

            userPreferencesRepository
                .getMaxStopDist()
                .flowOn(Dispatchers.Default)
                .onEach { minStopDist ->
                    _mapsUiState.update {
                        it.copy(minStopDist = minStopDist)
                    }
                }.launchIn(viewModelScope)

            viewModelScope.launch {
            }
        }

        /**
         * loads any vars in ui state that hasn't been loaded
         * THIS IGNORES THE RUNNING BUSES AS THIS SHOULD BE SUBSCRIBED TO FROM UI
         * */
        fun loadAll() {
            if (mapsUiState.value.routes.isEmpty()) {
                loadRoutes()
            }
            if (mapsUiState.value.schedule == null) {
                loadSchedule()
            }
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
        private fun loadBuses() {
            viewModelScope.launch {
                busesState =
                    apiRepository
                        .getBuses()
                        .map { response ->
                            readApiResponse(response) { buses ->
                                _mapsUiState.update {
                                    it.copy(buses = buses.values.toList())
                                }
                            }
                        }.shareIn(
                            viewModelScope,
                            SharingStarted.WhileSubscribed(5000),
                            1,
                        )
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

        private fun loadSchedule() {
            viewModelScope.launch {
                readApiResponse(apiRepository.getSchedule()) { response ->
                    _mapsUiState.update {
                        it.copy(schedule = response)
                    }
                }
            }
        }

        fun updateMapType(mapType: MapType) {
            viewModelScope.launch {
                userPreferencesRepository.saveMapType(mapType)
                _mapsUiState.update {
                    it.copy(mapType = mapType)
                }
            }
        }

        fun toggleMapType() {
            val next =
                if (mapsUiState.value.mapType == MapType.NORMAL) {
                    MapType.HYBRID
                } else {
                    MapType.NORMAL
                }

            updateMapType(next)
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
    }

/**
 * Representation of the screen
 * */
@Immutable
data class MapsUiState(
    val buses: List<Bus> = listOf(),
    val routes: Map<String, Route> = emptyMap(),
    val schedule: Schedule? = null,
    val networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    val serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    val unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
    val notificationsRead: Int = -1,
    val totalAnnouncements: Int = -1,
    val colorBlindMode: Boolean = false,
    val minStopDist: Float = 50f,
    val themeMode: ThemeMode = ThemeMode.System,
    val mapType: MapType = MapType.NORMAL,
)
