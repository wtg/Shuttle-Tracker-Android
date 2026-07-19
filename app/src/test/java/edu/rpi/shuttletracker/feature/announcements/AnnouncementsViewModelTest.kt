package edu.rpi.shuttletracker.feature.announcements

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import edu.rpi.shuttletracker.testing.fixtures.testAnnouncement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShuttleRepository
    private lateinit var preferences: FakeUserPreferences

    @Before
    fun setUp() {
        repository = FakeShuttleRepository()
        preferences = FakeUserPreferences()
    }

    @Test
    fun `successful announcements are newest first and marked read`() =
        runTest {
            repository.announcementsResult =
                NetworkResult.Success(listOf(testAnnouncement("Old"), testAnnouncement("New")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(
                viewModel.announcementsUiState.value.announcements
                    .map { it.subject },
            ).containsExactly("New", "Old")
                .inOrder()
            assertThat(preferences.notificationsRead.value).isEqualTo(2)
        }

    @Test
    fun `empty response finishes loading without an error`() =
        runTest {
            repository.announcementsResult = NetworkResult.Success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.announcementsUiState.value.isLoading).isFalse()
            assertThat(viewModel.announcementsUiState.value.announcements).isEmpty()
            assertThat(preferences.notificationsRead.value).isEqualTo(0)
        }

    @Test
    fun `server failure is exposed and loading ends`() =
        runTest {
            repository.announcementsResult = NetworkResult.Failure(NetworkError.Http(503, "maintenance"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(
                viewModel.announcementsUiState.value.serverError
                    ?.statusCode,
            ).isEqualTo(503)
            assertThat(viewModel.announcementsUiState.value.isLoading).isFalse()
        }

    @Test
    fun `retry clears failure and loads announcements`() =
        runTest {
            repository.announcementsResult = NetworkResult.Failure(NetworkError.NoConnection())
            val viewModel = createViewModel()
            advanceUntilIdle()
            repository.announcementsResult = NetworkResult.Success(listOf(testAnnouncement("Restored")))

            viewModel.retry()
            advanceUntilIdle()

            assertThat(viewModel.announcementsUiState.value.networkError).isNull()
            assertThat(
                viewModel.announcementsUiState.value.announcements
                    .single()
                    .subject,
            ).isEqualTo("Restored")
            assertThat(repository.announcementsCalls).isEqualTo(2)
        }

    @Test
    fun `clear errors retains loaded announcements`() =
        runTest {
            repository.announcementsResult = NetworkResult.Failure(NetworkError.Unknown())
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.clearErrors()

            assertThat(viewModel.announcementsUiState.value.unknownError).isNull()
            assertThat(viewModel.announcementsUiState.value.announcements).isEmpty()
        }

    private fun createViewModel() = AnnouncementsViewModel(repository, preferences)
}
