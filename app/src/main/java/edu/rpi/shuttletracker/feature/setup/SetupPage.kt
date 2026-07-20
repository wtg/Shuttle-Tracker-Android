package edu.rpi.shuttletracker.feature.setup

import androidx.annotation.StringRes
import edu.rpi.shuttletracker.R

/** One step of the first-run setup flow, in order: [About] -> [PrivacyPolicy] -> [Permissions]. */
enum class SetupPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val nextButtonRes: Int,
) {
    About(R.string.about, R.string.setup_accept),
    PrivacyPolicy(R.string.privacy_policy, R.string.setup_accept),
    Permissions(R.string.permissions, R.string.setup_finish),
    ;

    fun previous(): SetupPage =
        when (this) {
            About -> About
            PrivacyPolicy -> About
            Permissions -> PrivacyPolicy
        }
}
