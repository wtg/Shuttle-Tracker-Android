package edu.rpi.shuttletracker.feature.settings.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.AnalyticsFactory
import edu.rpi.shuttletracker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel
    @Inject
    constructor(
        private val analyticsFactory: AnalyticsFactory,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        fun getAnalytics() = analyticsFactory.build()

        fun updateAnalytics(update: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.saveAllowAnalytics(update)
            }
        }

        fun getAnalyticsEnabled() = userPreferencesRepository.getAllowAnalytics()
    }
