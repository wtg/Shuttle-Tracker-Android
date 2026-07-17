package edu.rpi.shuttletracker.feature.announcements

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.EmptyEvent
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.repository.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        // represents the ui state of the view
        private val _announcementsUiState = MutableStateFlow(AnnouncementsUIState())
        val announcementsUiState: StateFlow<AnnouncementsUIState> = _announcementsUiState

        init {
            loadAll()
            viewModelScope.launch { apiRepository.sendAnalytics(Event(announcementsListOpened = EmptyEvent)) }
        }

        fun loadAll() {
            if (announcementsUiState.value.announcements.isEmpty()) {
                getAnnouncements()
            }
        }

        /**
         * sets all the errors to none
         * */
        fun clearErrors() {
            loadAll()
            _announcementsUiState.update {
                it.copy(
                    unknownError = null,
                    networkError = null,
                    serverError = null,
                )
            }
        }

        /**
         * gets all the announcements and updates the amount the user has "read"
         * */
        private fun getAnnouncements() {
            viewModelScope.launch {
                readApiResponse(apiRepository.getAnnouncements()) { response ->
                    _announcementsUiState.update {
                        it.copy(announcements = response.reversed())
                    }

                    updateNotificationsRead()
                }
            }
        }

        /**
         * updates the number of notifications "read" with the amount of notifications there are
         * */
        private fun updateNotificationsRead() {
            // updates the amount of notifications read
            viewModelScope.launch {
                userPreferencesRepository
                    .saveNotificationsRead(
                        announcementsUiState.value.announcements.size,
                    )
            }
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
                            _announcementsUiState.update { it.copy(networkError = error) }
                        is NetworkError.Http ->
                            _announcementsUiState.update { it.copy(serverError = error) }
                        is NetworkError.Unknown ->
                            _announcementsUiState.update { it.copy(unknownError = error) }
                    }
            }
        }
    }

@Immutable
data class AnnouncementsUIState(
    val announcements: List<Announcement> = listOf(),
    val networkError: NetworkError.Connectivity? = null,
    val serverError: NetworkError.Http? = null,
    val unknownError: NetworkError.Unknown? = null,
)
