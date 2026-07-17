package edu.rpi.shuttletracker.feature.setup

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.MapsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SetupScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SetupScreen(
    navigator: DestinationsNavigator,
    viewModel: SetupScreenViewModel = hiltViewModel(),
) {
    var currentPage by remember { mutableIntStateOf(0) }

    BackHandler(currentPage > 0) {
        --currentPage
    }

    val pages =
        listOf(
            SetupPages.About(viewModel::updateAboutAccepted),
            SetupPages.PrivacyPolicy(viewModel::updatePrivacyPolicyAccepted),
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

                        if (currentPage == pages.lastIndex) {
                            viewModel.completeSetup()
                            navigator.navigate(MapsScreenDestination) {
                                popUpTo(SetupScreenDestination) { inclusive = true }
                            }
                        } else {
                            currentPage++
                        }
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
    Box(modifier = Modifier.fillMaxSize()) { Text(text = stringResource(R.string.privacy_page)) }
}

@Composable
@Preview
fun PermissionsPage() {
    LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionBox(permission = Permission.Notification)
        }

        PermissionBox(permission = Permission.Location)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            PermissionBox(permission = Permission.BackgroundLocation)
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            PermissionBox(permission = Permission.Bluetooth)
//        }
//
//        HorizontalDivider(modifier = Modifier.padding(10.dp))
//
//
//        Text(
//            text = "Requires background location and bluetooth permissions",
//            style = MaterialTheme.typography.labelSmall,
//        )
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
            allGranted = permissions.all { it.value }
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
