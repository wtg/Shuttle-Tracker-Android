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
import edu.rpi.shuttletracker.feature.map.utils.pickFakeShuttleRoute
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

@HiltViewModel
class MapsViewModel
// represents the ui state of the view
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

        init {
            loadAll()
            loadPreferences()
        }

        private fun loadAll() {
            if (mapsUiState.value.routes.isEmpty()) loadRoutes()
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
        }

        /**
         * Polls announcements on its own low-frequency interval, independent of vehicle polling, so a
         * banner refresh never has to compete with the 5 second vehicle updates.
         * */
        fun startAnnouncementRefresh() {
            if (announcementsJob?.isActive == true) return

            announcementsJob =
                shuttleRepository
                    .observeAnnouncements(pollMs = ANNOUNCEMENT_POLL_MS)
                    .onEach { result ->
                        // The dev menu's simulated banners take priority until it's turned back off.
                        if (mapsUiState.value.simulateAnnouncements) return@onEach

                        // A failed refresh must not clear announcements already on screen.
                        readApiResponse(result) { announcements ->
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
        }

        /**
         * Loops two vehicles around whichever route has enough coordinates to form a loop,
         * publishing them to [MapsUiState.fakeVehicles] so they never mix with real vehicle data
         * from [MapsUiState.vehicles].
         * */
        private fun startFakeVehicles() {
            if (fakeVehiclesJob?.isActive == true) return

            fakeVehiclesJob =
                viewModelScope.launch {
                    var elapsedMs = 0L
                    while (isActive) {
                        val fakeVehicles =
                            pickFakeShuttleRoute(mapsUiState.value.routes)?.let { (routeName, route) ->
                                buildFakeVehicles(routeName, route, elapsedMs)
                            } ?: emptyList()

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
                    readApiResponse(shuttleRepository.getRoutes()) { routes ->
                        _mapsUiState.update {
                            it.copy(routes = routes)
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
                .getSimulateAnnouncements()
                .onEach { simulate ->
                    val wasSimulating = mapsUiState.value.simulateAnnouncements
                    _mapsUiState.update { it.copy(simulateAnnouncements = simulate) }

                    if (simulate) {
                        _mapsUiState.update { it.copy(announcements = FakeAnnouncements.sample().displayable()) }
                    } else if (wasSimulating) {
                        // Get a fresh real fetch immediately rather than waiting for the next poll tick.
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

        /**
         * Reads the network response and maps it to correct place
         * */
        private fun <T> readApiResponse(
            response: NetworkResult<T>,
            success: (body: T) -> Unit,
        ) {
            when (response) {
                is NetworkResult.Success -> success(response.data)
                is NetworkResult.Failure ->
                    when (val error = response.error) {
                        is NetworkError.Connectivity ->
                            _mapsUiState.update { it.copy(networkError = error) }
                        is NetworkError.Http ->
                            _mapsUiState.update { it.copy(serverError = error) }
                        is NetworkError.Unknown ->
                            _mapsUiState.update { it.copy(unknownError = error) }
                    }
            }
        }
    }

private const val ANNOUNCEMENT_POLL_MS = 5 * 60 * 1000L
private const val FAKE_VEHICLE_TICK_MS = 1_000L

@Immutable
data class MapsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fakeVehicles: List<Vehicle> = emptyList(),
    val routes: Map<String, Route> = emptyMap(),
    val announcements: List<Announcement> = emptyList(),
    val announcementsUpdatedAt: Instant? = null,
    val simulateAnnouncements: Boolean = false,
    val networkError: NetworkError.Connectivity? = null,
    val serverError: NetworkError.Http? = null,
    val unknownError: NetworkError.Unknown? = null,
    val themeMode: ThemeMode = ThemeMode.System,
    val mapType: MapType = MapType.NORMAL,
    val shuttleAnimationsEnabled: Boolean = false,
    val shuttleRotationEnabled: Boolean = true,
)
