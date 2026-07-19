package edu.rpi.shuttletracker.feature.map

import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.MapType
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testSchedule
import edu.rpi.shuttletracker.testing.fixtures.testVehicleEta
import edu.rpi.shuttletracker.testing.fixtures.testVehicleLocation
import edu.rpi.shuttletracker.testing.fixtures.testVehicleVelocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShuttleRepository
    private lateinit var preferences: FakeUserPreferences

    @Before
    fun setUp() {
        repository =
            FakeShuttleRepository().apply {
                routesResult = NetworkResult.Success(mapOf("NORTH" to testRoute()))
                scheduleResult = NetworkResult.Success(testSchedule())
            }
        preferences = FakeUserPreferences()
    }

    @Test
    fun `initial load exposes routes schedule and preferences`() =
        runTest {
            preferences.mapType.value = MapType.HYBRID

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.routes).containsKey("NORTH")
            assertThat(viewModel.mapsUiState.value.schedule).isNotNull()
            assertThat(viewModel.mapsUiState.value.mapType).isEqualTo(MapType.HYBRID)
            assertThat(viewModel.mapsUiState.value.isScheduleLoading).isFalse()
        }

    @Test
    fun `vehicle polling merges the three endpoint results`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startVehiclePolling()
            repository.vehicleLocations.emit(NetworkResult.Success(mapOf("bus-1" to testVehicleLocation())))
            repository.vehicleEtas.emit(NetworkResult.Success(mapOf("bus-1" to testVehicleEta())))
            repository.vehicleVelocities.emit(NetworkResult.Success(mapOf("bus-1" to testVehicleVelocity())))
            advanceUntilIdle()

            val vehicle =
                viewModel.mapsUiState.value.vehicles
                    .single()
            assertThat(vehicle.routeName).isEqualTo("NORTH")
            assertThat(vehicle.stopTimes).containsKey("union")
        }

    @Test
    fun `starting polling twice creates only one set of streams`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.startVehiclePolling()
            viewModel.startVehiclePolling()

            assertThat(repository.observeLocationsCalls).isEqualTo(1)
            assertThat(repository.observeEtasCalls).isEqualTo(1)
            assertThat(repository.observeVelocitiesCalls).isEqualTo(1)
        }

    @Test
    fun `stopping polling prevents later vehicle updates`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startVehiclePolling()
            repository.vehicleLocations.emit(NetworkResult.Success(mapOf("bus-1" to testVehicleLocation())))
            repository.vehicleEtas.emit(NetworkResult.Success(emptyMap()))
            repository.vehicleVelocities.emit(NetworkResult.Success(emptyMap()))
            advanceUntilIdle()
            viewModel.stopVehiclePolling()

            repository.vehicleLocations.emit(NetworkResult.Success(mapOf("bus-2" to testVehicleLocation("West Bus"))))
            repository.vehicleEtas.emit(NetworkResult.Success(emptyMap()))
            repository.vehicleVelocities.emit(NetworkResult.Success(emptyMap()))
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.vehicles
                    .single()
                    .id,
            ).isEqualTo("bus-1")
        }

    @Test
    fun `connectivity failure is exposed in UI state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startVehiclePolling()
            repository.vehicleLocations.emit(NetworkResult.Failure(NetworkError.NoConnection()))
            repository.vehicleEtas.emit(NetworkResult.Success(emptyMap()))
            repository.vehicleVelocities.emit(NetworkResult.Success(emptyMap()))
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.networkError).isInstanceOf(NetworkError.NoConnection::class.java)
        }

    @Test
    fun `retry clears the error and reloads missing routes`() =
        runTest {
            repository.routesResult = NetworkResult.Failure(NetworkError.Unknown())
            val viewModel = createViewModel()
            advanceUntilIdle()
            repository.routesResult = NetworkResult.Success(mapOf("NORTH" to testRoute()))

            viewModel.retry()
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.unknownError).isNull()
            assertThat(viewModel.mapsUiState.value.routes).containsKey("NORTH")
            assertThat(repository.routesCalls).isEqualTo(2)
        }

    @Test
    fun `toggling map type persists the next value`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleMapType()
            advanceUntilIdle()

            assertThat(preferences.mapType.value).isEqualTo(MapType.HYBRID)
            assertThat(viewModel.mapsUiState.value.mapType).isEqualTo(MapType.HYBRID)
        }

    private fun createViewModel() = MapsViewModel(repository, preferences)
}
