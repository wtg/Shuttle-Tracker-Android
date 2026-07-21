package edu.rpi.shuttletracker.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.remote.ShuttleRemoteDataSource
import edu.rpi.shuttletracker.testing.fixtures.testAnnouncement
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testSchedule
import edu.rpi.shuttletracker.testing.fixtures.testVehicleLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultShuttleRepositoryTest {
    private val remote = FakeRemoteShuttleDataSource()
    private val repository = DefaultShuttleRepository(remote)

    @Test
    fun `polling emits immediately and repeats after the interval`() =
        runTest {
            repository.observeVehicleLocations(pollMs = 100).test {
                assertThat((awaitItem() as NetworkResult.Success).data).containsKey("bus-1")
                assertThat(remote.locationCalls).isEqualTo(1)

                advanceTimeBy(100)
                runCurrent()

                awaitItem()
                assertThat(remote.locationCalls).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cancelling polling stops future remote calls`() =
        runTest {
            repository.observeVehicleLocations(pollMs = 100).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            advanceTimeBy(500)
            runCurrent()

            assertThat(remote.locationCalls).isEqualTo(1)
        }

    @Test
    fun `one-shot repository calls delegate to the remote data source`() =
        runTest {
            repository.getRoutes()
            repository.getAnnouncements()
            repository.getSchedule()

            assertThat(remote.routesCalls).isEqualTo(1)
            assertThat(remote.announcementCalls).isEqualTo(1)
            assertThat(remote.scheduleCalls).isEqualTo(1)
        }

    private class FakeRemoteShuttleDataSource : ShuttleRemoteDataSource {
        var locationCalls = 0
        var routesCalls = 0
        var announcementCalls = 0
        var scheduleCalls = 0

        override suspend fun getVehicleLocations(): NetworkResult<Map<String, VehicleLocation>> {
            locationCalls++
            return NetworkResult.Success(mapOf("bus-1" to testVehicleLocation()))
        }

        override suspend fun getVehicleEtas(): NetworkResult<Map<String, VehicleStopEta>> =
            NetworkResult.Success(emptyMap())

        override suspend fun getVehicleVelocities(): NetworkResult<Map<String, VehicleVelocities>> =
            NetworkResult.Success(emptyMap())

        override suspend fun getRoutes(): NetworkResult<Map<String, Route>> {
            routesCalls++
            return NetworkResult.Success(mapOf("NORTH" to testRoute()))
        }

        override suspend fun getAnnouncements(): NetworkResult<List<Announcement>> {
            announcementCalls++
            return NetworkResult.Success(listOf(testAnnouncement("Update")))
        }

        override suspend fun getSchedule(): NetworkResult<Schedule> {
            scheduleCalls++
            return NetworkResult.Success(testSchedule())
        }
    }
}
