package edu.rpi.shuttletracker.ui.setup

import androidx.compose.runtime.Composable

const val TOTAL_PAGES = 4

/**
 * Contains all the screens the setup process will have and simple information to display about each
 * */
sealed class SetupPages(
    val title: String,
    val nextText: String,
    val content: @Composable () -> Unit,
    val onComplete: () -> Unit = {},
) {
    data class About(
        val acceptAbout: () -> Unit,
    ) : SetupPages(
            "About",
            "I accept",
            { AboutPage() },
            { acceptAbout() },
        )

    data class PrivacyPolicy(
        val acceptPrivatePolicy: () -> Unit,
    ) : SetupPages(
            "Privacy Policy",
            "I accept",
            { PrivacyPolicyPage() },
            { acceptPrivatePolicy() },
        )

    data class Analytics(
        val allowAnalytics: () -> Unit,
        val analyticsEnabled: Boolean,
    ) : SetupPages(
            "Analytics",
            "Next",
            { AnalyticsPage(allowAnalytics, analyticsEnabled) },
        )

    data object Permissions : SetupPages(
        "Permissions",
        "Finish",
        { PermissionsPage() },
    )
}
