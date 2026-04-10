package edu.rpi.shuttletracker.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.maps.android.compose.MapType
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * To create a new preference (setting item)
 * add a new static key with the name of the key in snake case
 * Then create a setter and getter for the value, giving it a default value in the getter
 * */
class UserPreferencesRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val apiRepository: Lazy<ApiRepository>,
        @param:ApplicationContext private val context: Context,
    ) {
        companion object {
            private val USER_ID = stringPreferencesKey("user_id")
            private val NOTIFICATIONS_READ = intPreferencesKey("notifications_read")
            private val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
            private val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
            private val ABOUT_ACCEPTED = booleanPreferencesKey("about_accepted")
            private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
            private val MAX_STOP_DIST = floatPreferencesKey("max_stop_dist")
            private val BASE_URL = stringPreferencesKey("base_url")
            private val ALLOW_ANALYTICS = booleanPreferencesKey("allow_analytics")
            private val DEV_OPTIONS_ACTIVE = booleanPreferencesKey("dev_options_active")
            private val THEME_MODE = stringPreferencesKey("theme_mode")
            private val MAP_TYPE = stringPreferencesKey("map_type")
            private val SHUTTLE_ANIMATIONS = booleanPreferencesKey("shuttle-animations")
            private val SHUTTLE_ROTATION = booleanPreferencesKey("shuttle_rotation")
        }

        suspend fun getUserId(): String =
            dataStore.data
                .map { preference ->
                    if (preference[USER_ID] == null) {
                        dataStore.edit {
                            it[USER_ID] = UUID.randomUUID().toString()
                        }
                    }

                    preference[USER_ID] ?: UUID.randomUUID().toString()
                }.first()

        fun getNotificationsRead(): Flow<Int> =
            dataStore.data.map {
                it[NOTIFICATIONS_READ] ?: 0
            }

        suspend fun saveNotificationsRead(count: Int) {
            dataStore.edit {
                it[NOTIFICATIONS_READ] = count
            }
        }

        fun getColorBlindMode(): Flow<Boolean> =
            dataStore.data.map {
                it[COLOR_BLIND_MODE] ?: false
            }

        suspend fun saveColorBlindMode(colorBlindMode: Boolean) {
            dataStore.edit {
                it[COLOR_BLIND_MODE] = colorBlindMode
            }

            apiRepository.get().sendAnalytics(Event(colorBlindModeToggled = colorBlindMode))
        }

        fun getMapType(): Flow<MapType> =
            dataStore.data.map {
                when (it[MAP_TYPE]) {
                    MapType.HYBRID.name -> MapType.HYBRID
                    else -> MapType.NORMAL
                }
            }

        suspend fun saveMapType(mapType: MapType) {
            dataStore.edit {
                it[MAP_TYPE] = mapType.name
            }
        }

        fun getPrivacyPolicyAccepted(): Flow<Boolean> =
            dataStore.data.map {
                it[PRIVACY_POLICY_ACCEPTED] ?: false
            }

        suspend fun savePrivacyPolicyAccepted(privacyPolicyAccepted: Boolean) {
            dataStore.edit {
                it[PRIVACY_POLICY_ACCEPTED] = privacyPolicyAccepted
            }
        }

        fun getAboutAccepted(): Flow<Boolean> =
            dataStore.data.map {
                it[ABOUT_ACCEPTED] ?: false
            }

        suspend fun saveAboutAccepted(aboutAccepted: Boolean) {
            dataStore.edit {
                it[ABOUT_ACCEPTED] = aboutAccepted
            }
        }

        fun getSetupCompleted(): Flow<Boolean> =
            dataStore.data.map {
                it[SETUP_COMPLETED] ?: false
            }

        suspend fun saveSetupCompleted(setupCompleted: Boolean) {
            dataStore.edit {
                it[SETUP_COMPLETED] = setupCompleted
            }
        }

        fun getMaxStopDist(): Flow<Float> =
            dataStore.data.map {
                it[MAX_STOP_DIST] ?: 20f
            }

        suspend fun saveMaxStopDist(minStopDist: Float) {
            dataStore.edit {
                it[MAX_STOP_DIST] = minStopDist
            }
        }

        fun getBaseUrl(): Flow<String> =
            dataStore.data.map {
                it[BASE_URL] ?: context.getString(R.string.url_default)
            }

        suspend fun saveBaseUrl(url: String) {
            dataStore.edit {
                it[BASE_URL] = url
            }
        }

        fun getAllowAnalytics(): Flow<Boolean> =
            dataStore.data.map {
                it[ALLOW_ANALYTICS] ?: false
            }

        suspend fun saveAllowAnalytics(allowAnalytics: Boolean) {
            dataStore.edit {
                it[ALLOW_ANALYTICS] = allowAnalytics
            }
        }

        suspend fun activateDevOptions(devOptionEnable: Boolean) {
            dataStore.edit {
                it[DEV_OPTIONS_ACTIVE] = devOptionEnable
            }
        }

        fun getDevOptions(): Flow<Boolean> =
            dataStore.data.map {
                it[DEV_OPTIONS_ACTIVE] ?: false
            }

        fun getThemeMode(): Flow<ThemeMode> =
            dataStore.data.map {
                when (it[THEME_MODE]) {
                    ThemeMode.Light.name -> ThemeMode.Light
                    ThemeMode.Dark.name -> ThemeMode.Dark
                    else -> ThemeMode.System
                }
            }

        suspend fun saveThemeMode(themeMode: ThemeMode) {
            dataStore.edit {
                it[THEME_MODE] = themeMode.name
            }
        }

        suspend fun clearAllPreferences() {
            dataStore.edit { prefs ->
                val userId = prefs[USER_ID]
                prefs.clear()
                if (userId != null) {
                    prefs[USER_ID] = userId
                }
            }
        }

        fun getShuttleAnimations(): Flow<Boolean> = 
            dataStore.data.map {
                it[SHUTTLE_ANIMATIONS] ?:
        true
            }

        suspend fun saveShuttleAnimations(animationsEnable: Boolean) {
            dataStore.edit {
                it[SHUTTLE_ANIMATIONS] = animationsEnable
            }
        }

        fun getShuttleRotation(): Flow<Boolean> =
            dataStore.data.map {
                it[SHUTTLE_ROTATION] ?: false
        }

        suspend fun saveShuttleRotations(rotationsEnable: Boolean) {
            dataStore.edit {
                it[SHUTTLE_ROTATION] = rotationsEnable
        }
    }
        
    }
