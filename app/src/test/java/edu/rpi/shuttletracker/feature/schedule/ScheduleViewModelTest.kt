package edu.rpi.shuttletracker.feature.schedule

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
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
                scheduleResult = NetworkResult.Success(testSchedule())
            }
    }

    @Test
    fun `initial load caches schedule`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.scheduleUiState.value.schedule).isNotNull()
            assertThat(viewModel.scheduleUiState.value.isScheduleLoading).isFalse()
            assertThat(repository.scheduleCalls).isEqualTo(1)
        }

    @Test
    fun `each view model loads schedule once`() =
        runTest {
            createViewModel()
            advanceUntilIdle()
            createViewModel()
            advanceUntilIdle()

            assertThat(repository.scheduleCalls).isEqualTo(2)
        }

    @Test
    fun `a failed load is exposed in ui state`() =
        runTest {
            repository.scheduleResult = NetworkResult.Failure(NetworkError.Unknown())
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.scheduleUiState.value.error).isNotNull()
        }

    @Test
    fun `retry clears the error and reloads missing schedule`() =
        runTest {
            repository.scheduleResult = NetworkResult.Failure(NetworkError.Unknown())
            val viewModel = createViewModel()
            advanceUntilIdle()
            repository.scheduleResult = NetworkResult.Success(testSchedule())

            viewModel.retry()
            advanceUntilIdle()

            assertThat(viewModel.scheduleUiState.value.error).isNull()
            assertThat(viewModel.scheduleUiState.value.schedule).isNotNull()
            assertThat(repository.scheduleCalls).isEqualTo(2)
        }

    private fun createViewModel() = ScheduleViewModel(repository)
}
