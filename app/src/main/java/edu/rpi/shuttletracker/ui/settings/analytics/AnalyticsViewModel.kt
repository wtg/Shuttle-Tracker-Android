package edu.rpi.shuttletracker.ui.settings.analytics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rpi.shuttletracker.data.models.AnalyticsFactory
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsFactory: AnalyticsFactory,
) : ViewModel() {

    fun getAnalytics() =
        analyticsFactory.build(true)
}
