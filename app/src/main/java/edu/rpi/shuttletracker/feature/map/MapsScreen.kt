package edu.rpi.shuttletracker.feature.map

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.feature.etas.EtasScreen
import edu.rpi.shuttletracker.feature.etas.EtasViewModel
import edu.rpi.shuttletracker.feature.map.components.AnnouncementSheet
import edu.rpi.shuttletracker.feature.schedule.ScheduleScreen
import edu.rpi.shuttletracker.feature.schedule.ScheduleViewModel

/**
 * Peer destinations of the live tracker experience. Switched with local state rather than a
 * Navigation3 route since they share one Scaffold and bottom bar.
 * */
private enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Map(R.string.nav_map, R.drawable.ic_explore),
    Etas(R.string.nav_etas, R.drawable.ic_directions_bus),
    Schedule(R.string.schedule_title, R.drawable.ic_schedule),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    onOpenSettings: () -> Unit,
    viewModel: MapsViewModel = hiltViewModel(),
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
    etasViewModel: EtasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.mapsUiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Map) }
    var isAnnouncementsSheetVisible by rememberSaveable { mutableStateOf(false) }
    val announcementsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        snackbarHost = {
            CheckResponseError(
                uiState.networkError,
                uiState.serverError,
                uiState.unknownError,
                ignoreErrorRequest = viewModel::clearErrors,
                retryErrorRequest = viewModel::retry,
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { contentPadding ->
        when (selectedTab) {
            MainTab.Map ->
                MapTab(
                    viewModel = viewModel,
                    uiState = uiState,
                    contentPadding = contentPadding,
                    onSettingsClick = onOpenSettings,
                    isAnnouncementsSheetVisible = isAnnouncementsSheetVisible,
                    onAnnouncementsSheetVisibleChange = { isAnnouncementsSheetVisible = it },
                    announcementsSheetState = announcementsSheetState,
                )

            MainTab.Etas ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    EtasScreen(viewModel = etasViewModel)
                }

            MainTab.Schedule ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    ScheduleScreen(viewModel = scheduleViewModel)
                }
        }
    }
}

/**
 * Vehicle and announcement polling are scoped to this composable's own lifetime, not the whole
 * screen's, so switching to another tab actually stops the live 5-second polling instead of
 * leaving it running in the background indefinitely.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTab(
    viewModel: MapsViewModel,
    uiState: MapsUiState,
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    isAnnouncementsSheetVisible: Boolean,
    onAnnouncementsSheetVisibleChange: (Boolean) -> Unit,
    announcementsSheetState: SheetState,
) {
    LifecycleStartEffect(viewModel) {
        viewModel.startVehiclePolling()
        viewModel.startAnnouncementRefresh()
        onStopOrDispose {
            viewModel.stopVehiclePolling()
            viewModel.stopAnnouncementRefresh()
        }
    }

    Box(Modifier.fillMaxSize()) {
        ShuttleMap(
            uiState = uiState,
            contentPadding = contentPadding,
            onSettingsClick = onSettingsClick,
            onToggleMapTypeClick = viewModel::toggleMapType,
            onAnnouncementsClick = { onAnnouncementsSheetVisibleChange(true) },
        )

        AnnouncementSheet(
            show = isAnnouncementsSheetVisible,
            sheetState = announcementsSheetState,
            announcements = uiState.announcements,
            updatedAt = if (uiState.simulateAnnouncements) null else uiState.announcementsUpdatedAt,
            onDismiss = { onAnnouncementsSheetVisibleChange(false) },
        )
    }
}
