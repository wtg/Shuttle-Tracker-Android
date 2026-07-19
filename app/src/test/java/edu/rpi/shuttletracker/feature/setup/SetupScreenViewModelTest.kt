package edu.rpi.shuttletracker.feature.setup

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.testing.coroutine.MainDispatcherRule
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferences: FakeUserPreferences
    private lateinit var viewModel: SetupScreenViewModel

    @Before
    fun setUp() {
        preferences = FakeUserPreferences()
        viewModel = SetupScreenViewModel(preferences)
    }

    @Test
    fun `setup starts on the about page`() {
        assertThat(viewModel.uiState.value).isEqualTo(SetupUiState())
    }

    @Test
    fun `accepting about persists acceptance before advancing`() =
        runTest {
            viewModel.completeCurrentPage()
            advanceUntilIdle()

            assertThat(preferences.aboutAccepted.value).isTrue()
            assertThat(viewModel.uiState.value.page).isEqualTo(SetupPage.PrivacyPolicy)
        }

    @Test
    fun `accepting privacy persists acceptance before permissions`() =
        runTest {
            viewModel.completeCurrentPage()
            advanceUntilIdle()
            viewModel.completeCurrentPage()
            advanceUntilIdle()

            assertThat(preferences.privacyPolicyAccepted.value).isTrue()
            assertThat(viewModel.uiState.value.page).isEqualTo(SetupPage.Permissions)
        }

    @Test
    fun `finishing permissions completes setup`() =
        runTest {
            repeat(3) {
                viewModel.completeCurrentPage()
                advanceUntilIdle()
            }

            assertThat(preferences.setupCompleted.value).isTrue()
            assertThat(viewModel.uiState.value.isComplete).isTrue()
        }

    @Test
    fun `repeated action while saving is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            preferences.aboutSaveGate = gate

            viewModel.completeCurrentPage()
            runCurrent()
            viewModel.completeCurrentPage()
            runCurrent()

            assertThat(preferences.saveAboutAcceptedCalls).isEqualTo(1)
            gate.complete(Unit)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.page).isEqualTo(SetupPage.PrivacyPolicy)
        }
}
