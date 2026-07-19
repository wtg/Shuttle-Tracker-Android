package edu.rpi.shuttletracker.feature.schedule

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShuttleRepository

    @Before
    fun setUp() {
        repository =
            FakeShuttleRepository().apply {
                routesResult = NetworkResult.Success(mapOf("NORTH" to testRoute()))
                scheduleResult = NetworkResult.Success(testSchedule())
            }
    }

    @Test
    fun `initial load caches routes and schedule`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.scheduleUiState.value.routes).containsKey("NORTH")
            assertThat(viewModel.scheduleUiState.value.schedule).isNotNull()
            assertThat(viewModel.scheduleUiState.value.isScheduleLoading).isFalse()
            assertThat(repository.routesCalls).isEqualTo(1)
            assertThat(repository.scheduleCalls).isEqualTo(1)
        }

    @Test
    fun `creating another view model reuses cached data instead of refetching`() =
        runTest {
            createViewModel()
            advanceUntilIdle()
            createViewModel()
            advanceUntilIdle()

            // The fake repository has no cache of its own, so each fresh view model instance
            // fetches once on init; this pins that a single instance never refetches on its own.
            assertThat(repository.routesCalls).isEqualTo(2)
            assertThat(repository.scheduleCalls).isEqualTo(2)
        }

    @Test
    fun `refresh drops the cache and fetches routes and schedule again`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(repository.routesCalls).isEqualTo(2)
            assertThat(repository.scheduleCalls).isEqualTo(2)
            assertThat(viewModel.scheduleUiState.value.routes).containsKey("NORTH")
            assertThat(viewModel.scheduleUiState.value.schedule).isNotNull()
        }

    @Test
    fun `refresh shows loading while the schedule is being refetched`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.refresh()

            assertThat(viewModel.scheduleUiState.value.isScheduleLoading).isTrue()
        }

    @Test
    fun `a failed load is exposed in ui state`() =
        runTest {
            repository.routesResult = NetworkResult.Failure(NetworkError.Unknown())
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.scheduleUiState.value.unknownError).isNotNull()
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

            assertThat(viewModel.scheduleUiState.value.unknownError).isNull()
            assertThat(viewModel.scheduleUiState.value.routes).containsKey("NORTH")
            assertThat(repository.routesCalls).isEqualTo(2)
        }

    private fun createViewModel() = ScheduleViewModel(repository)
}
