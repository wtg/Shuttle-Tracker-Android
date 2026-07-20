package edu.rpi.shuttletracker.feature.map

import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.MapType
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.AnnouncementType
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import edu.rpi.shuttletracker.testing.fixtures.testAnnouncement
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testVehicleEta
import edu.rpi.shuttletracker.testing.fixtures.testVehicleLocation
import edu.rpi.shuttletracker.testing.fixtures.testVehicleVelocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

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
                routesResult = NetworkResult.Success(mapOf("NORTH" to testRoute(), "WEST" to testRoute()))
            }
        preferences = FakeUserPreferences()
    }

    @Test
    fun `initial load exposes routes and preferences`() =
        runTest {
            preferences.mapType.value = MapType.HYBRID

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.routes).containsKey("NORTH")
            assertThat(viewModel.mapsUiState.value.mapType).isEqualTo(MapType.HYBRID)
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
    fun `announcement refresh loads and filters to active unexpired announcements`() =
        runTest {
            val now = Instant.parse("2026-06-01T00:00:00Z")
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()

            repository.announcements.emit(
                NetworkResult.Success(
                    listOf(
                        testAnnouncement("active", active = true, expiresAt = null),
                        testAnnouncement("inactive", active = false),
                        testAnnouncement("expired", active = true, expiresAt = now.minusSeconds(60)),
                    ),
                ),
            )
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.announcements
                    .map { it.id },
            ).containsExactly("active")
        }

    @Test
    fun `announcement refresh keeps previously loaded announcements after a failure`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()

            repository.announcements.emit(NetworkResult.Success(listOf(testAnnouncement("first"))))
            advanceUntilIdle()
            repository.announcements.emit(NetworkResult.Failure(NetworkError.NoConnection()))
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.announcements
                    .map { it.id },
            ).containsExactly("first")
            assertThat(viewModel.mapsUiState.value.networkError).isInstanceOf(NetworkError.NoConnection::class.java)
        }

    @Test
    fun `starting announcement refresh twice creates only one stream`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.startAnnouncementRefresh()
            viewModel.startAnnouncementRefresh()

            assertThat(repository.observeAnnouncementsCalls).isEqualTo(1)
        }

    @Test
    fun `stopping announcement refresh does not couple to vehicle polling`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()
            viewModel.startVehiclePolling()

            viewModel.stopAnnouncementRefresh()
            repository.vehicleLocations.emit(NetworkResult.Success(mapOf("bus-1" to testVehicleLocation())))
            repository.vehicleEtas.emit(NetworkResult.Success(emptyMap()))
            repository.vehicleVelocities.emit(NetworkResult.Success(emptyMap()))
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.vehicles).isNotEmpty()
        }

    @Test
    fun `multiple active announcements are exposed sorted by severity`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()

            repository.announcements.emit(
                NetworkResult.Success(
                    listOf(
                        testAnnouncement("info", type = AnnouncementType.Info),
                        testAnnouncement("error", type = AnnouncementType.Error),
                        testAnnouncement("warning", type = AnnouncementType.Warning),
                    ),
                ),
            )
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.announcements
                    .map { it.id },
            ).containsExactly("error", "warning", "info")
                .inOrder()
        }

    @Test
    fun `enabling simulate announcements shows the fake sample set`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()

            preferences.simulateAnnouncements.value = true
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.announcements
                    .map { it.id },
            ).containsExactly("fake-error", "fake-warning", "fake-info")
                .inOrder()
        }

    @Test
    fun `simulated announcements are not overwritten by a real refresh tick`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()
            preferences.simulateAnnouncements.value = true
            advanceUntilIdle()

            repository.announcements.emit(NetworkResult.Success(listOf(testAnnouncement("real"))))
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.announcements
                    .map { it.id },
            ).containsExactly("fake-error", "fake-warning", "fake-info")
                .inOrder()
        }

    @Test
    fun `disabling simulate announcements triggers a fresh real fetch`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()
            preferences.simulateAnnouncements.value = true
            advanceUntilIdle()

            preferences.simulateAnnouncements.value = false
            repository.announcements.emit(NetworkResult.Success(listOf(testAnnouncement("real"))))
            advanceUntilIdle()

            assertThat(
                viewModel.mapsUiState.value.announcements
                    .map { it.id },
            ).containsExactly("real")
            assertThat(repository.observeAnnouncementsCalls).isEqualTo(2)
        }

    @Test
    fun `a successful announcement refresh records when it happened`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()

            assertThat(viewModel.mapsUiState.value.announcementsUpdatedAt).isNull()

            repository.announcements.emit(NetworkResult.Success(listOf(testAnnouncement("first"))))
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.announcementsUpdatedAt).isNotNull()
        }

    @Test
    fun `a failed announcement refresh does not update the timestamp`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.startAnnouncementRefresh()
            repository.announcements.emit(NetworkResult.Success(listOf(testAnnouncement("first"))))
            advanceUntilIdle()
            val updatedAt = viewModel.mapsUiState.value.announcementsUpdatedAt

            repository.announcements.emit(NetworkResult.Failure(NetworkError.NoConnection()))
            advanceUntilIdle()

            assertThat(viewModel.mapsUiState.value.announcementsUpdatedAt).isEqualTo(updatedAt)
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

    // The fake vehicle ticker loops forever with delay(), so advanceUntilIdle() would hang while
    // it's running; runCurrent()/advanceTimeBy() step the virtual clock by a bounded amount instead.

    @Test
    fun `fake vehicles only start once both dev options and the fake shuttle toggle are on`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            preferences.devOptions.value = true
            runCurrent()
            assertThat(viewModel.mapsUiState.value.fakeVehicles).isEmpty()

            preferences.devOptions.value = false
            preferences.fakeShuttlesEnabled.value = true
            runCurrent()
            assertThat(viewModel.mapsUiState.value.fakeVehicles).isEmpty()

            preferences.devOptions.value = true
            runCurrent()
            assertThat(viewModel.mapsUiState.value.fakeVehicles).hasSize(2)

            preferences.fakeShuttlesEnabled.value = false
            runCurrent()
        }

    @Test
    fun `fake vehicles are kept out of the real vehicles list and keep moving over time`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            preferences.devOptions.value = true
            preferences.fakeShuttlesEnabled.value = true
            runCurrent()

            val firstTick =
                viewModel.mapsUiState.value.fakeVehicles
                    .first()
            assertThat(viewModel.mapsUiState.value.vehicles).isEmpty()

            advanceTimeBy(30_000)
            runCurrent()

            val laterTick =
                viewModel.mapsUiState.value.fakeVehicles
                    .first()
            assertThat(laterTick.latitude != firstTick.latitude || laterTick.longitude != firstTick.longitude).isTrue()

            preferences.fakeShuttlesEnabled.value = false
            runCurrent()
        }

    @Test
    fun `turning fake shuttles back off clears them from ui state`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            preferences.devOptions.value = true
            preferences.fakeShuttlesEnabled.value = true
            runCurrent()
            assertThat(viewModel.mapsUiState.value.fakeVehicles).isNotEmpty()

            preferences.fakeShuttlesEnabled.value = false
            runCurrent()

            assertThat(viewModel.mapsUiState.value.fakeVehicles).isEmpty()
        }

    private fun createViewModel() = MapsViewModel(repository, preferences)
}
