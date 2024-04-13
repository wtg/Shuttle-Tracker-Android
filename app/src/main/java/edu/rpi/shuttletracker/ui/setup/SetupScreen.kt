package edu.rpi.shuttletracker.ui.setup

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.destinations.MapsScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.util.services.BeaconService
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun SetupScreen(
    navigator: DestinationsNavigator,
    viewModel: SetupScreenViewModel = hiltViewModel(),
    manualStart: Boolean = false,
) {
    val startPage = if (manualStart) 0 else runBlocking { viewModel.getStartPage() }
    if (startPage == TOTAL_PAGES) {
        navigator.navigate(MapsScreenDestination) {
            popUpTo(SetupScreenDestination) {
                inclusive = true
            }
        }
    }

    var currentPage by remember { mutableIntStateOf(startPage) }

    BackHandler(currentPage > 0) {
        --currentPage
    }

    val pages =
        listOf(
            SetupPages.About(viewModel::updateAboutAccepted),
            SetupPages.PrivacyPolicy(viewModel::updatePrivacyPolicyAccepted),
            SetupPages.Analytics(
                viewModel::updateAllowAnalytics,
                viewModel.getAnalyticsEnabled().collectAsStateWithLifecycle(
                    initialValue = false,
                ).value,
            ),
            SetupPages.Permissions,
        )

    LaunchedEffect(key1 = currentPage) {
        if (currentPage == pages.size) {
            navigator.navigate(MapsScreenDestination) {
                popUpTo(SetupScreenDestination) {
                    inclusive = true
                }
            }
        }
    }
    if (currentPage < pages.size) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = pages[currentPage].title) },
            )
        }) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(it),
            ) {
                LazyColumn(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(10.dp),
                ) {
                    item {
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            modifier = Modifier.animateContentSize(),
                        ) {
                            Crossfade(targetState = currentPage, label = "fade") { page ->
                                Box(modifier = Modifier.padding(10.dp)) {
                                    pages[page].content()
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        pages[currentPage].onComplete()
                        ++currentPage
                    }) {
                        Text(text = pages[currentPage].nextText)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutPage() {
    Box(modifier = Modifier.fillMaxSize()) { Text(text = stringResource(R.string.about_page)) }
}

@Composable
fun PrivacyPolicyPage() {
    Box(modifier = Modifier.fillMaxSize()) { Text(text = stringResource(R.string.privacy)) }
}

@Composable
fun AnalyticsPage(
    allowAnalytics: () -> Unit,
    analyticsEnabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.analytics_policy))

        Spacer(modifier = Modifier.padding(30.dp))

        if (!analyticsEnabled) {
            Button(onClick = { allowAnalytics() }) {
                Text(text = "Enable analytics")
            }
        } else {
            Text(text = "Analytics is enabled")
        }
    }
}

@Composable
@Preview
fun PermissionsPage() {
    val autoBoardingRunning = BeaconService.isRunning.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionBox(permission = Permission.Notification)
        }

        PermissionBox(permission = Permission.Location)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionBox(permission = Permission.BackgroundLocation)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionBox(permission = Permission.Bluetooth)
        }

        HorizontalDivider(modifier = Modifier.padding(10.dp))

        Button(onClick = {
            context.startForegroundService(Intent(context, BeaconService::class.java))
        }, enabled = !autoBoardingRunning) {
            Text(text = if (!autoBoardingRunning) "Enable auto-boarding" else "Auto-boarding enabled")
        }

        Text(
            text = "Requires background location and bluetooth permissions",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * Used to check whether or not the user has permissions for permissions
 * And if not, will prompt the user to enable them
 * */
@Composable
fun PermissionBox(permission: Permission) {
    val context = LocalContext.current
    var allGranted by remember {
        mutableStateOf(
            permission.permissions.all {
                ContextCompat.checkSelfPermission(
                    context,
                    it,
                ) == PackageManager.PERMISSION_GRANTED
            },
        )
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            allGranted =
                permissions.values.reduce { acc, permissionGranted ->
                    acc && permissionGranted
                }
        }

    Row(
        modifier = Modifier.padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = permission.name, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text = permission.description, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.padding(20.dp))

        Button(onClick = { launcher.launch(permission.permissions) }, enabled = !allGranted) {
            Text(text = if (!allGranted) "Grant" else "Granted")
        }
    }
}
