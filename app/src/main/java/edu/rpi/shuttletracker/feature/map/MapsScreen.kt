package edu.rpi.shuttletracker.feature.map

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.feature.etas.EtasScreen
import edu.rpi.shuttletracker.feature.map.components.AnnouncementSheet
import edu.rpi.shuttletracker.feature.schedule.ScheduleScreen
import edu.rpi.shuttletracker.feature.schedule.ScheduleViewModel

/** Home tabs share one scaffold, so they use local pager state instead of navigation routes. */
private enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Map(R.string.nav_map, R.drawable.ic_explore),
    Etas(R.string.nav_etas, R.drawable.ic_directions_bus),
    Schedule(R.string.schedule_title, R.drawable.ic_schedule),
}

/** Hosts the Map, ETA, and Schedule tabs while preserving each tab's UI state. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MapsScreen(
    onOpenSettings: () -> Unit,
    viewModel: MapsViewModel = hiltViewModel(),
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.mapsUiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { MainTab.entries.size }
    val selectedTab = MainTab.entries[pagerState.currentPage]
    var isAnnouncementsSheetVisible by rememberSaveable { mutableStateOf(false) }
    var focusedVehicleId by remember { mutableStateOf<String?>(null) }
    val announcementsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Wider layouts move navigation to a side rail.
    val windowSizeClass = calculateWindowSizeClass(requireNotNull(LocalActivity.current))
    val useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // Match dark navigation chrome to the map controls.
    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val navContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else null

    LifecycleStartEffect(viewModel, selectedTab) {
        if (selectedTab != MainTab.Schedule) viewModel.startVehiclePolling()
        if (selectedTab == MainTab.Map) viewModel.startAnnouncementRefresh()
        onStopOrDispose {
            viewModel.stopVehiclePolling()
            viewModel.stopAnnouncementRefresh()
        }
    }

    Row(Modifier.fillMaxSize()) {
        if (useNavigationRail) {
            NavigationRail {
                MainTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = selectedTab == tab,
                        onClick = { pagerState.requestScrollToPage(tab.ordinal) },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            snackbarHost = {
                CheckResponseError(
                    uiState.error,
                    ignoreErrorRequest = viewModel::clearErrors,
                    retryErrorRequest = viewModel::retry,
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(containerColor = navContainerColor ?: NavigationBarDefaults.containerColor) {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { pagerState.requestScrollToPage(tab.ordinal) },
                                icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { contentPadding ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = MainTab.entries.lastIndex,
                userScrollEnabled = false,
            ) { page ->
                when (MainTab.entries[page]) {
                    MainTab.Map ->
                        MapTab(
                            viewModel = viewModel,
                            uiState = uiState,
                            contentPadding = contentPadding,
                            onSettingsClick = onOpenSettings,
                            isAnnouncementsSheetVisible = isAnnouncementsSheetVisible,
                            onAnnouncementsSheetVisibleChange = { isAnnouncementsSheetVisible = it },
                            announcementsSheetState = announcementsSheetState,
                            focusedVehicleId = focusedVehicleId,
                            onFocusedVehicleHandled = { focusedVehicleId = null },
                        )

                    MainTab.Etas ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                        ) {
                            // A rail already labels the selected tab.
                            EtasScreen(
                                routes = uiState.routes,
                                vehicles = uiState.vehicles + uiState.fakeVehicles,
                                routesLoaded = uiState.routesLoaded,
                                onVehicleClick = { vehicleId ->
                                    focusedVehicleId = vehicleId
                                    pagerState.requestScrollToPage(MainTab.Map.ordinal)
                                },
                                showTitle = !useNavigationRail,
                            )
                        }

                    MainTab.Schedule ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                        ) {
                            ScheduleScreen(
                                viewModel = scheduleViewModel,
                                routesByName = uiState.routes,
                                showTitle = !useNavigationRail,
                                isWideLayout = useNavigationRail,
                            )
                        }
                }
            }
        }
    }
}

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
    focusedVehicleId: String?,
    onFocusedVehicleHandled: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ShuttleMap(
            uiState = uiState,
            contentPadding = contentPadding,
            onSettingsClick = onSettingsClick,
            onToggleMapTypeClick = viewModel::toggleMapType,
            onAnnouncementsClick = { onAnnouncementsSheetVisibleChange(true) },
            focusedVehicleId = focusedVehicleId,
            onFocusedVehicleHandled = onFocusedVehicleHandled,
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
