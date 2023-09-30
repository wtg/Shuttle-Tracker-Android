package edu.rpi.shuttletracker.ui.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    private val apiRepository: ShuttleTrackerRepository,
) : ViewModel() {

    // represents the ui state of the view
    private val _announcementsUiState = MutableStateFlow(AnnouncementsUIState())
    val announcementsUiState: StateFlow<AnnouncementsUIState> = _announcementsUiState

    init {
        loadAll()
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

    private fun getAnnouncements() {
        viewModelScope.launch {
            readApiResponse(apiRepository.getAnnouncements()) { response ->
                _announcementsUiState.update {
                    it.copy(announcements = response.reversed())
                }
            }
        }
    }

    /**
     * Reads the network response and maps it to correct place
     * */
    private fun <T> readApiResponse(
        response: NetworkResponse<T, ErrorResponse>,
        success: (body: T) -> Unit,
    ) {
        when (response) {
            is NetworkResponse.Success -> success(response.body)
            is NetworkResponse.ServerError -> _announcementsUiState.update { it.copy(serverError = response) }
            is NetworkResponse.NetworkError -> _announcementsUiState.update { it.copy(networkError = response) }
            is NetworkResponse.UnknownError -> _announcementsUiState.update { it.copy(unknownError = response) }
        }
    }
}

data class AnnouncementsUIState(
    val announcements: List<Announcement> = listOf(),
    val networkError: NetworkResponse.NetworkError<*, ErrorResponse>? = null,
    val serverError: NetworkResponse.ServerError<*, ErrorResponse>? = null,
    val unknownError: NetworkResponse.UnknownError<*, ErrorResponse>? = null,
)
