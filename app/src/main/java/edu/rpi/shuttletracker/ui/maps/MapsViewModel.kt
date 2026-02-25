package edu.rpi.shuttletracker.ui.maps

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.MapType
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.vehicle.VehicleLocation
import edu.rpi.shuttletracker.data.models.vehicle.VehicleStopEta
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

        init {
            loadAll()
            observeVehicleLocations()
            observeVehicleEtas()
            loadPreferences()
        }

        fun loadAll() {
            if (mapsUiState.value.routes.isEmpty()) loadRoutes()
            if (mapsUiState.value.schedule == null) loadSchedule()
        }

        /**
         * sets all the errors to none
         * */
        fun clearErrors() {
            _mapsUiState.update {
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

        private fun observeVehicleLocations() {
            apiRepository
                .observeVehicleLocations(pollMs = 5_000L)
                .flowOn(Dispatchers.IO)
                .onEach { response ->
                    readApiResponse(response) { buses ->
                        _mapsUiState.update {
                            it.copy(vehicleLocations = buses)
                        }
                    }
                }.launchIn(viewModelScope)
        }

        private fun observeVehicleEtas() {
            apiRepository
                .observeVehicleEtas(pollMs = 5_000L)
                .flowOn(Dispatchers.IO)
                .onEach { response ->
                    readApiResponse(response) { etas ->
                        _mapsUiState.update {
                            it.copy(vehicleStopEtas = etas)
                        }
                    }
                }.launchIn(viewModelScope)
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

        private fun loadPreferences() {
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
    val vehicleLocations: Map<String, VehicleLocation> = emptyMap(),
    val vehicleStopEtas: Map<String, VehicleStopEta> = emptyMap(),
    val routes: Map<String, Route> = emptyMap(),
    val schedule: Schedule? = null,
    val networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    val serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    val unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
    val notificationsRead: Int = -1,
    val totalAnnouncements: Int = -1,
    val themeMode: ThemeMode = ThemeMode.System,
    val mapType: MapType = MapType.NORMAL,
)
