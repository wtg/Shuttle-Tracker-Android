package edu.rpi.shuttletracker.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiRepository: ShuttleTrackerRepository,
) : ViewModel()
