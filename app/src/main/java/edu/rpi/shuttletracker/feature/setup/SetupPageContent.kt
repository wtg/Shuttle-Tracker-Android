package edu.rpi.shuttletracker.feature.setup

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.feature.setup.components.PermissionItem

/** Renders whichever [SetupPage] the user is currently on. */
@Composable
fun SetupPageContent(page: SetupPage) {
    when (page) {
        SetupPage.About -> AboutPage()
        SetupPage.PrivacyPolicy -> PrivacyPolicyPage()
        SetupPage.Permissions -> PermissionsPage()
    }
}

@Composable
fun AboutPage() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.about_page),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * [R.array.privacy_page] is one paragraph per item, so each gets its own [Text] with real spacing
 * instead of running together. The first item (title + effective date) is de-emphasized since
 * [SetupPage]'s TopAppBar already shows "Privacy Policy" as the page title.
 * */
@Composable
fun PrivacyPolicyPage() {
    val paragraphs = stringArrayResource(R.array.privacy_page)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        paragraphs.firstOrNull()?.let { header ->
            Text(
                text = header,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        paragraphs.drop(1).forEach { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun PermissionsPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionBox(permission = Permission.Notification)
        }

        PermissionBox(permission = Permission.Location)
    }
}

/** One [Permission]'s row: tracks whether it's granted and launches the system permission dialog on tap. */
@Composable
fun PermissionBox(permission: Permission) {
    val context = LocalContext.current
    var isGranted by remember(permission, context) {
        mutableStateOf(permission.isGranted(context))
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            isGranted = permission.isGranted(context)
        }

    PermissionItem(
        name = stringResource(permission.nameRes),
        description = stringResource(permission.descriptionRes),
        isGranted = isGranted,
        grantLabel = stringResource(R.string.setup_grant),
        grantedLabel = stringResource(R.string.setup_granted),
        onRequestPermission = { launcher.launch(permission.permissions) },
    )
}

private fun Permission.isGranted(context: Context): Boolean {
    val grantResults =
        permissions.map { permissionName ->
            ContextCompat.checkSelfPermission(context, permissionName) ==
                PackageManager.PERMISSION_GRANTED
        }

    return if (requiresAll) grantResults.all { it } else grantResults.any { it }
}
