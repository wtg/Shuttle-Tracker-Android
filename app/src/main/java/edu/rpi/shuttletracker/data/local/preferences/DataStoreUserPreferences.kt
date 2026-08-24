package edu.rpi.shuttletracker.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.maps.android.compose.MapType
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** DataStore-backed settings. Add new settings here, in [UserPreferences], and in its test fakes. */
class DataStoreUserPreferences
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserPreferences {
        companion object {
            private val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
            private val ABOUT_ACCEPTED = booleanPreferencesKey("about_accepted")
            private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
            private val DEV_OPTIONS_ACTIVE = booleanPreferencesKey("dev_options_active")
            private val SIMULATE_ANNOUNCEMENTS = booleanPreferencesKey("simulate_announcements")
            private val FAKE_SHUTTLES_ENABLED = booleanPreferencesKey("fake_shuttles_enabled")
            private val THEME_MODE = stringPreferencesKey("theme_mode")
            private val MAP_TYPE = stringPreferencesKey("map_type")
            private val SHUTTLE_ANIMATIONS = booleanPreferencesKey("shuttle-animations")
            private val SHUTTLE_ROTATION = booleanPreferencesKey("shuttle_rotation")
        }

        override fun getMapType(): Flow<MapType> =
            dataStore.data.map {
                when (it[MAP_TYPE]) {
                    MapType.HYBRID.name -> MapType.HYBRID
                    else -> MapType.NORMAL
                }
            }

        override suspend fun saveMapType(mapType: MapType) {
            dataStore.edit {
                it[MAP_TYPE] = mapType.name
            }
        }

        override fun getPrivacyPolicyAccepted(): Flow<Boolean> =
            dataStore.data.map {
                it[PRIVACY_POLICY_ACCEPTED] ?: false
            }

        override suspend fun savePrivacyPolicyAccepted(privacyPolicyAccepted: Boolean) {
            dataStore.edit {
                it[PRIVACY_POLICY_ACCEPTED] = privacyPolicyAccepted
            }
        }

        override fun getAboutAccepted(): Flow<Boolean> =
            dataStore.data.map {
                it[ABOUT_ACCEPTED] ?: false
            }

        override suspend fun saveAboutAccepted(aboutAccepted: Boolean) {
            dataStore.edit {
                it[ABOUT_ACCEPTED] = aboutAccepted
            }
        }

        override fun getSetupCompleted(): Flow<Boolean> =
            dataStore.data.map {
                it[SETUP_COMPLETED] ?: false
            }

        override suspend fun saveSetupCompleted(setupCompleted: Boolean) {
            dataStore.edit {
                it[SETUP_COMPLETED] = setupCompleted
            }
        }

        override suspend fun activateDevOptions(devOptionEnable: Boolean) {
            dataStore.edit {
                it[DEV_OPTIONS_ACTIVE] = devOptionEnable
            }
        }

        override fun getDevOptions(): Flow<Boolean> =
            dataStore.data.map {
                it[DEV_OPTIONS_ACTIVE] ?: false
            }

        override fun getSimulateAnnouncements(): Flow<Boolean> =
            dataStore.data.map {
                it[SIMULATE_ANNOUNCEMENTS] ?: false
            }

        override suspend fun saveSimulateAnnouncements(enabled: Boolean) {
            dataStore.edit {
                it[SIMULATE_ANNOUNCEMENTS] = enabled
            }
        }

        override fun getFakeShuttlesEnabled(): Flow<Boolean> =
            dataStore.data.map {
                it[FAKE_SHUTTLES_ENABLED] ?: false
            }

        override suspend fun saveFakeShuttlesEnabled(enabled: Boolean) {
            dataStore.edit {
                it[FAKE_SHUTTLES_ENABLED] = enabled
            }
        }

        override fun getThemeMode(): Flow<ThemeMode> =
            dataStore.data.map {
                when (it[THEME_MODE]) {
                    ThemeMode.Light.name -> ThemeMode.Light
                    ThemeMode.Dark.name -> ThemeMode.Dark
                    else -> ThemeMode.System
                }
            }

        override suspend fun saveThemeMode(themeMode: ThemeMode) {
            dataStore.edit {
                it[THEME_MODE] = themeMode.name
            }
        }

        override suspend fun resetSetup() {
            dataStore.edit { preferences ->
                preferences.remove(ABOUT_ACCEPTED)
                preferences.remove(PRIVACY_POLICY_ACCEPTED)
                preferences.remove(SETUP_COMPLETED)
            }
        }

        override fun getShuttleAnimations(): Flow<Boolean> =
            dataStore.data.map {
                it[SHUTTLE_ANIMATIONS]
                    ?: false
            }

        override suspend fun saveShuttleAnimations(animationsEnable: Boolean) {
            dataStore.edit {
                it[SHUTTLE_ANIMATIONS] = animationsEnable
            }
        }

        override fun getShuttleRotation(): Flow<Boolean> =
            dataStore.data.map {
                it[SHUTTLE_ROTATION] ?: true
            }

        override suspend fun saveShuttleRotations(rotationsEnable: Boolean) {
            dataStore.edit {
                it[SHUTTLE_ROTATION] = rotationsEnable
            }
        }
    }
