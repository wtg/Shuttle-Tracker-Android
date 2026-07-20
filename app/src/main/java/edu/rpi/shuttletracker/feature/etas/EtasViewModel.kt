package edu.rpi.shuttletracker.feature.etas

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleMerger
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [EtasScreen]. Loads [routes] once (like [edu.rpi.shuttletracker.feature.map.MapsViewModel]
 * does), and polls the same three vehicle endpoints while [startVehiclePolling] is active, merging
 * them into [EtasUiState.vehicles]. [feature.etas.utils.buildStopsWithEtas] then turns
 * routes + vehicles into the actual per-stop list the screen shows.
 * */
@HiltViewModel
class EtasViewModel
    @Inject
    constructor(
        private val shuttleRepository: ShuttleRepository,
    ) : ViewModel() {
        private val _etasUiState = MutableStateFlow(EtasUiState())
        val etasUiState: StateFlow<EtasUiState> = _etasUiState.asStateFlow()
        private var vehiclePollingJob: Job? = null
        private var routesJob: Job? = null

        init {
            if (etasUiState.value.routes.isEmpty()) loadRoutes()
        }

        fun startVehiclePolling() {
            if (vehiclePollingJob?.isActive == true) return

            vehiclePollingJob =
                combine(
                    shuttleRepository.observeVehicleLocations(pollMs = 5_000L),
                    shuttleRepository.observeVehicleEtas(pollMs = 5_000L),
                    shuttleRepository.observeVehicleVelocities(pollMs = 5_000L),
                ) { locationsResponse, etasResponse, velocitiesResponse ->
                    Triple(locationsResponse, etasResponse, velocitiesResponse)
                }.onEach { (locationsResponse, etasResponse, velocitiesResponse) ->
                    var locations: Map<String, VehicleLocation> = emptyMap()
                    var etas: Map<String, VehicleStopEta> = emptyMap()
                    var velocities: Map<String, VehicleVelocities> = emptyMap()

                    readApiResponse(locationsResponse) { locations = it }
                    readApiResponse(etasResponse) { etas = it }
                    readApiResponse(velocitiesResponse) { velocities = it }

                    _etasUiState.update {
                        it.copy(
                            vehicles =
                                VehicleMerger.merge(
                                    locations = locations,
                                    velocities = velocities,
                                    etas = etas,
                                ),
                        )
                    }
                }.launchIn(viewModelScope)
        }

        fun stopVehiclePolling() {
            vehiclePollingJob?.cancel()
            vehiclePollingJob = null
        }

        fun selectRouteFilter(routeName: String?) {
            _etasUiState.update { it.copy(selectedRouteFilter = routeName) }
        }

        fun selectStop(stopKey: String?) {
            _etasUiState.update { it.copy(selectedStopKey = stopKey) }
        }

        fun clearErrors() {
            _etasUiState.update {
                it.copy(
                    unknownError = null,
                    networkError = null,
                    serverError = null,
                )
            }
        }

        fun retry() {
            clearErrors()
            if (etasUiState.value.routes.isEmpty()) loadRoutes()
        }

        private fun loadRoutes() {
            if (routesJob?.isActive == true) return
            routesJob =
                viewModelScope.launch {
                    readApiResponse(shuttleRepository.getRoutes()) { routes ->
                        _etasUiState.update {
                            it.copy(routes = routes)
                        }
                    }
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
                            _etasUiState.update { it.copy(networkError = error) }
                        is NetworkError.Http ->
                            _etasUiState.update { it.copy(serverError = error) }
                        is NetworkError.Unknown ->
                            _etasUiState.update { it.copy(unknownError = error) }
                    }
            }
        }
    }

@Immutable
data class EtasUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val routes: Map<String, Route> = emptyMap(),
    val selectedRouteFilter: String? = null,
    val selectedStopKey: String? = null,
    val networkError: NetworkError.Connectivity? = null,
    val serverError: NetworkError.Http? = null,
    val unknownError: NetworkError.Unknown? = null,
)
