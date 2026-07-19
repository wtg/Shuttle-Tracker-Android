package edu.rpi.shuttletracker.feature.settings.about

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.Typography
import edu.rpi.shuttletracker.core.ui.theme.shuttleTrackerColorScheme

@Composable
fun LibrariesScreen(onOpened: () -> Unit) {
    val context = LocalContext.current
    val title = stringResource(R.string.libraries_used)

    LaunchedEffect(context, title) {
        OssLicensesMenuActivity.setActivityTitle(title)
        OssLicensesMenuActivity.setTheme(
            shuttleTrackerColorScheme(context, darkTheme = false),
            shuttleTrackerColorScheme(context, darkTheme = true),
            Typography,
        )
        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        onOpened()
    }
}
