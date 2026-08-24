package edu.rpi.shuttletracker.feature.map

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.MapType
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleMerger
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.models.displayable
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import edu.rpi.shuttletracker.feature.map.utils.buildFakeVehicles
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Owns shared route, vehicle, announcement, and preference state for the home tabs. */
@HiltViewModel
class MapsViewModel
    @Inject
    constructor(
        private val shuttleRepository: ShuttleRepository,
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        private val _mapsUiState = MutableStateFlow(MapsUiState())
        val mapsUiState: StateFlow<MapsUiState> = _mapsUiState.asStateFlow()
        private var vehiclePollingJob: Job? = null
        private var routesJob: Job? = null
        private var announcementsJob: Job? = null
        private var fakeVehiclesJob: Job? = null
        private var failedRequest: MapRequest? = null

        init {
            loadAll()
            loadPreferences()
        }

        private fun loadAll() {
            if (mapsUiState.value.routes.isEmpty()) loadRoutes()
        }

        fun clearErrors() {
            failedRequest = null
            _mapsUiState.update { it.copy(error = null) }
        }

        fun retry() {
            val request = failedRequest
            clearErrors()
            when (request) {
                MapRequest.Routes -> loadRoutes()
                MapRequest.Vehicles -> {
                    if (vehiclePollingJob != null) {
                        stopVehiclePolling()
                        startVehiclePolling()
                    }
                }
                MapRequest.Announcements -> {
                    if (announcementsJob != null) {
                        stopAnnouncementRefresh()
                        startAnnouncementRefresh()
                    }
                }
                null -> loadAll()
            }
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

                    // Clear vehicle errors only when a live vehicle endpoint recovers.
                    if (locationsResponse is NetworkResult.Success ||
                        etasResponse is NetworkResult.Success ||
                        velocitiesResponse is NetworkResult.Success
                    ) {
                        clearError(MapRequest.Vehicles)
                        _mapsUiState.update {
                            it.copy(
                                error = null,
                                vehiclesUpdatedAt = Instant.now(),
                            )
                        }
                    }

                    readApiResponse(locationsResponse, MapRequest.Vehicles) { locations = it }
                    readApiResponse(etasResponse, MapRequest.Vehicles) { etas = it }
                    readApiResponse(velocitiesResponse, MapRequest.Vehicles) { velocities = it }

                    _mapsUiState.update {
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
            clearError(MapRequest.Vehicles)
        }

        /** Polls announcements independently from frequent vehicle updates. */
        fun startAnnouncementRefresh() {
            if (announcementsJob?.isActive == true) return

            announcementsJob =
                shuttleRepository
                    .observeAnnouncements(pollMs = ANNOUNCEMENT_POLL_MS)
                    .onEach { result ->
                        // Simulated banners take priority while enabled.
                        if (mapsUiState.value.simulateAnnouncements) return@onEach

                        // A failed refresh must not clear announcements already on screen.
                        readApiResponse(result, MapRequest.Announcements) { announcements ->
                            clearError(MapRequest.Announcements)
                            _mapsUiState.update {
                                it.copy(
                                    announcements = announcements.displayable(),
                                    announcementsUpdatedAt = Instant.now(),
                                )
                            }
                        }
                    }.launchIn(viewModelScope)
        }

        fun stopAnnouncementRefresh() {
            announcementsJob?.cancel()
            announcementsJob = null
            clearError(MapRequest.Announcements)
        }

        /** Updates developer-mode shuttles once per second without mixing them into live data. */
        private fun startFakeVehicles() {
            if (fakeVehiclesJob?.isActive == true) return

            fakeVehiclesJob =
                viewModelScope.launch {
                    var elapsedMs = 0L
                    while (isActive) {
                        val fakeVehicles = buildFakeVehicles(mapsUiState.value.routes, elapsedMs)

                        _mapsUiState.update { it.copy(fakeVehicles = fakeVehicles) }

                        delay(FAKE_VEHICLE_TICK_MS)
                        elapsedMs += FAKE_VEHICLE_TICK_MS
                    }
                }
        }

        private fun stopFakeVehicles() {
            fakeVehiclesJob?.cancel()
            fakeVehiclesJob = null
            _mapsUiState.update { it.copy(fakeVehicles = emptyList()) }
        }

        private fun loadRoutes() {
            if (routesJob?.isActive == true) return
            routesJob =
                viewModelScope.launch {
                    readApiResponse(shuttleRepository.getRoutes(), MapRequest.Routes) { routes ->
                        clearError(MapRequest.Routes)
                        _mapsUiState.update {
                            it.copy(routes = routes, routesLoaded = true)
                        }
                    }
                }
        }

        private fun loadPreferences() {
            userPreferences
                .getThemeMode()
                .onEach { themeMode ->
                    _mapsUiState.update {
                        it.copy(themeMode = themeMode)
                    }
                }.launchIn(viewModelScope)

            userPreferences
                .getMapType()
                .onEach { mapType ->
                    _mapsUiState.update {
                        it.copy(mapType = mapType)
                    }
                }.launchIn(viewModelScope)

            userPreferences
                .getShuttleAnimations()
                .onEach { animationsEnable ->
                    _mapsUiState.update {
                        it.copy(shuttleAnimationsEnabled = animationsEnable)
                    }
                }.launchIn(viewModelScope)

            userPreferences
                .getShuttleRotation()
                .onEach { rotationEnable ->
                    _mapsUiState.update {
                        it.copy(shuttleRotationEnabled = rotationEnable)
                    }
                }.launchIn(viewModelScope)

            userPreferences
                .getDevOptions()
                .onEach { devOptionsEnabled ->
                    _mapsUiState.update {
                        it.copy(isDevModeEnabled = devOptionsEnabled)
                    }
                }.launchIn(viewModelScope)

            combine(
                userPreferences.getDevOptions(),
                userPreferences.getSimulateAnnouncements(),
            ) { devOptionsEnabled, simulate -> devOptionsEnabled && simulate }
                .distinctUntilChanged()
                .onEach { simulateActive ->
                    val wasSimulating = mapsUiState.value.simulateAnnouncements
                    _mapsUiState.update { it.copy(simulateAnnouncements = simulateActive) }

                    if (simulateActive) {
                        _mapsUiState.update { it.copy(announcements = FakeAnnouncements.sample().displayable()) }
                    } else if (wasSimulating) {
                        // Fetch real announcements immediately after simulation ends.
                        stopAnnouncementRefresh()
                        startAnnouncementRefresh()
                    }
                }.launchIn(viewModelScope)

            combine(
                userPreferences.getDevOptions(),
                userPreferences.getFakeShuttlesEnabled(),
            ) { devOptionsEnabled, fakeShuttlesEnabled -> devOptionsEnabled && fakeShuttlesEnabled }
                .distinctUntilChanged()
                .onEach { fakeShuttlesActive ->
                    if (fakeShuttlesActive) startFakeVehicles() else stopFakeVehicles()
                }.launchIn(viewModelScope)
        }

        private fun updateMapType(mapType: MapType) {
            viewModelScope.launch {
                userPreferences.saveMapType(mapType)
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
            response: NetworkResult<T>,
            request: MapRequest,
            success: (body: T) -> Unit,
        ) {
            when (response) {
                is NetworkResult.Success -> success(response.data)
                is NetworkResult.Failure -> {
                    failedRequest = request
                    _mapsUiState.update { it.copy(error = response.error) }
                }
            }
        }

        private fun clearError(request: MapRequest) {
            if (failedRequest == request) clearErrors()
        }
    }

private const val ANNOUNCEMENT_POLL_MS = 5 * 60 * 1000L
private const val FAKE_VEHICLE_TICK_MS = 1_000L

private enum class MapRequest {
    Routes,
    Vehicles,
    Announcements,
}

@Immutable
data class MapsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fakeVehicles: List<Vehicle> = emptyList(),
    val routes: Map<String, Route> = emptyMap(),
    val routesLoaded: Boolean = false,
    val announcements: List<Announcement> = emptyList(),
    val announcementsUpdatedAt: Instant? = null,
    val vehiclesUpdatedAt: Instant? = null,
    val simulateAnnouncements: Boolean = false,
    val error: NetworkError? = null,
    val themeMode: ThemeMode = ThemeMode.System,
    val mapType: MapType = MapType.NORMAL,
    val shuttleAnimationsEnabled: Boolean = false,
    val shuttleRotationEnabled: Boolean = true,
    val isDevModeEnabled: Boolean = false,
)
