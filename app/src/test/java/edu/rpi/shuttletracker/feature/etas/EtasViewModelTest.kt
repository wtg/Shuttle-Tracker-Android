package edu.rpi.shuttletracker.feature.etas

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testVehicleEta
import edu.rpi.shuttletracker.testing.fixtures.testVehicleLocation
import edu.rpi.shuttletracker.testing.fixtures.testVehicleVelocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EtasViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShuttleRepository
    private lateinit var preferences: FakeUserPreferences

    @Before
    fun setUp() {
        repository =
            FakeShuttleRepository().apply {
                routesResult = NetworkResult.Success(mapOf("NORTH" to testRoute()))
            }
        preferences = FakeUserPreferences()
    }

    @Test
    fun `initial load caches routes`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.etasUiState.value.routes).containsKey("NORTH")
            assertThat(repository.routesCalls).isEqualTo(1)
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
                viewModel.etasUiState.value.vehicles
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
                viewModel.etasUiState.value.vehicles
                    .single()
                    .id,
            ).isEqualTo("bus-1")
        }

    @Test
    fun `selecting a route filter and a stop updates ui state`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.selectRouteFilter("NORTH")
            viewModel.selectStop("union")

            assertThat(viewModel.etasUiState.value.selectedRouteFilter).isEqualTo("NORTH")
            assertThat(viewModel.etasUiState.value.selectedStopKey).isEqualTo("union")

            viewModel.selectStop(null)
            assertThat(viewModel.etasUiState.value.selectedStopKey).isNull()
        }

    @Test
    fun `connectivity failure is exposed in ui state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startVehiclePolling()
            repository.vehicleLocations.emit(NetworkResult.Failure(NetworkError.NoConnection()))
            repository.vehicleEtas.emit(NetworkResult.Success(emptyMap()))
            repository.vehicleVelocities.emit(NetworkResult.Success(emptyMap()))
            advanceUntilIdle()

            assertThat(viewModel.etasUiState.value.networkError).isInstanceOf(NetworkError.NoConnection::class.java)
        }

    @Test
    fun `empty routes response marks routes as loaded instead of leaving ui stuck loading`() =
        runTest {
            repository.routesResult = NetworkResult.Success(emptyMap())
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.etasUiState.value.routesLoaded).isTrue()
            assertThat(viewModel.etasUiState.value.routes).isEmpty()
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

            assertThat(viewModel.etasUiState.value.unknownError).isNull()
            assertThat(viewModel.etasUiState.value.routes).containsKey("NORTH")
            assertThat(repository.routesCalls).isEqualTo(2)
        }

    // The fake vehicle ticker loops forever with delay(), so advanceUntilIdle() would hang while
    // it's running; runCurrent() steps the virtual clock by a bounded amount instead.

    @Test
    fun `fake vehicles only start once both dev options and the fake shuttle toggle are on`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            preferences.devOptions.value = true
            runCurrent()
            assertThat(viewModel.etasUiState.value.fakeVehicles).isEmpty()

            preferences.fakeShuttlesEnabled.value = true
            runCurrent()
            assertThat(viewModel.etasUiState.value.fakeVehicles).hasSize(1)

            preferences.fakeShuttlesEnabled.value = false
            runCurrent()
        }

    @Test
    fun `fake vehicles carry synthesized stop etas so the etas tab has something to show`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            preferences.devOptions.value = true
            preferences.fakeShuttlesEnabled.value = true
            runCurrent()

            val fakeVehicle =
                viewModel.etasUiState.value.fakeVehicles
                    .single()
            assertThat(fakeVehicle.stopTimes.keys).containsExactly("union", "academy")

            preferences.fakeShuttlesEnabled.value = false
            runCurrent()
        }

    private fun createViewModel() = EtasViewModel(repository, preferences)
}
