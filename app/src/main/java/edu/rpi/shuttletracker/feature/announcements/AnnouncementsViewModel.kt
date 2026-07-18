package edu.rpi.shuttletracker.feature.announcements

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.repository.ApiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementsViewModel
    @Inject
    constructor(
        private val apiRepository: ApiRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _announcementsUiState = MutableStateFlow(AnnouncementsUiState())
        val announcementsUiState: StateFlow<AnnouncementsUiState> =
            _announcementsUiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            loadAll()
        }

        private fun loadAll() {
            if (announcementsUiState.value.announcements.isNotEmpty()) return
            if (loadJob?.isActive == true) return

            loadJob = loadAnnouncements()
        }

        /**
         * sets all the errors to none
         * */
        fun clearErrors() {
            _announcementsUiState.update {
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

        /**
         * gets all the announcements and updates the amount the user has "read"
         * */
        private fun loadAnnouncements(): Job =
            viewModelScope.launch {
                _announcementsUiState.update { it.copy(isLoading = true) }

                when (val response = apiRepository.getAnnouncements()) {
                    is NetworkResult.Success -> {
                        val announcements = response.data.reversed()
                        _announcementsUiState.update {
                            it.copy(
                                announcements = announcements,
                                isLoading = false,
                            )
                        }
                        userPreferencesRepository.saveNotificationsRead(announcements.size)
                    }

                    is NetworkResult.Failure -> {
                        updateError(response.error)
                        _announcementsUiState.update { it.copy(isLoading = false) }
                    }
                }
            }

        /**
         * Reads the network response and maps it to correct place
         * */
        private fun updateError(error: NetworkError) {
            _announcementsUiState.update { state ->
                when (error) {
                    is NetworkError.Connectivity -> state.copy(networkError = error)
                    is NetworkError.Http -> state.copy(serverError = error)
                    is NetworkError.Unknown -> state.copy(unknownError = error)
                }
            }
        }
    }

@Immutable
data class AnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = true,
    val networkError: NetworkError.Connectivity? = null,
    val serverError: NetworkError.Http? = null,
    val unknownError: NetworkError.Unknown? = null,
)
