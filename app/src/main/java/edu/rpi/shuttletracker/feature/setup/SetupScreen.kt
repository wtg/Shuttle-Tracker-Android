package edu.rpi.shuttletracker.feature.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme

/** Runs the first-launch About, Privacy Policy, and Permissions flow. */
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onSetupComplete()
        }
    }

    SetupContent(
        uiState = uiState,
        onPreviousPage = viewModel::goToPreviousPage,
        onCompletePage = viewModel::completeCurrentPage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetupContent(
    uiState: SetupUiState,
    onPreviousPage: () -> Unit,
    onCompletePage: () -> Unit,
) {
    BackHandler(uiState.page != SetupPage.About, onBack = onPreviousPage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(uiState.page.titleRes)) },
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
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
                        Crossfade(targetState = uiState.page, label = "setup page") { page ->
                            Box(modifier = Modifier.padding(10.dp)) {
                                SetupPageContent(page)
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
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCompletePage,
                ) {
                    Text(text = stringResource(uiState.page.nextButtonRes))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupContentPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        SetupContent(
            uiState = SetupUiState(),
            onPreviousPage = {},
            onCompletePage = {},
        )
    }
}
