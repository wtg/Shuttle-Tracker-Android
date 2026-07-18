package edu.rpi.shuttletracker.feature.setup

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.feature.setup.components.PermissionItem

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
        Text(text = stringResource(R.string.about_page))
    }
}

@Composable
fun PrivacyPolicyPage() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = stringResource(R.string.privacy_page))
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

private fun Permission.isGranted(context: android.content.Context): Boolean {
    val grantResults =
        permissions.map { permissionName ->
            ContextCompat.checkSelfPermission(context, permissionName) ==
                PackageManager.PERMISSION_GRANTED
        }

    return if (requiresAll) grantResults.all { it } else grantResults.any { it }
}
