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
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleMerger
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel
    // represents the ui state of the view
    @Inject
    constructor(
        private val apiRepository: ApiRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _mapsUiState = MutableStateFlow(MapsUiState())
        val mapsUiState: StateFlow<MapsUiState> = _mapsUiState

        init {
            loadAll()
            observeVehicles()
            loadPreferences()
        }

        fun loadAll() {
            if (mapsUiState.value.routes.isEmpty()) loadRoutes()
            if (mapsUiState.value.schedule == null) loadSchedule()
        }

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

        private fun observeVehicles() {
            combine(
                apiRepository.observeVehicleLocations(pollMs = 5_000L),
                apiRepository.observeVehicleEtas(pollMs = 5_000L),
                apiRepository.observeVehicleVelocities(pollMs = 5_000L),
            ) { locationsResponse, etasResponse, velocitiesResponse ->
                Triple(locationsResponse, etasResponse, velocitiesResponse)
            }.flowOn(Dispatchers.IO)
                .onEach { (locationsResponse, etasResponse, velocitiesResponse) ->
                    var locations: Map<String, VehicleLocation> = emptyMap()
                    var etas: Map<String, VehicleStopEta> = emptyMap()
                    var velocities: Map<String, VehicleVelocities> = emptyMap()

                    readApiResponse(locationsResponse) { locations = it }
                    readApiResponse(etasResponse) { etas = it }
                    readApiResponse(velocitiesResponse) { velocities = it }

                    val vehicles =
                        VehicleMerger.merge(
                            locations = locations,
                            velocities = velocities,
                            etas = etas,
                        )

                    _mapsUiState.update {
                        it.copy(
                            vehicles = vehicles,
                        )
                    }
                }.launchIn(viewModelScope)
        }

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

@Immutable
data class MapsUiState(
    val vehicles: List<Vehicle> = emptyList(),
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
