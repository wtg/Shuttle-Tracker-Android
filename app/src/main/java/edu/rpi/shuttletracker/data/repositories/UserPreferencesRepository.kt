package edu.rpi.shuttletracker.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.rpi.shuttletracker.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val USER_ID = stringPreferencesKey("user_id")
        private val NOTIFICATIONS_READ = intPreferencesKey("notifications_read")
        private val AUTO_BOARD_SERVICE = booleanPreferencesKey("auto_board_service")
        private val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
        private val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        private val ABOUT_ACCEPTED = booleanPreferencesKey("about_accepted")
        private val MAX_STOP_DIST = floatPreferencesKey("max_stop_dist")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val BOARD_BUS_COUNT = intPreferencesKey("board_bus_count")
        private val ALLOW_ANALYTICS = booleanPreferencesKey("allow_analytics")
    }

    suspend fun getUserId(): String = dataStore.data.map { preference ->
        if (preference[USER_ID] == null) {
            dataStore.edit {
                it[USER_ID] = UUID.randomUUID().toString()
            }
        }

        preference[USER_ID]
    }.first().toString()

    fun getNotificationsRead(): Flow<Int> = dataStore.data.map {
        it[NOTIFICATIONS_READ] ?: 0
    }

    suspend fun saveNotificationsRead(count: Int) {
        dataStore.edit {
            it[NOTIFICATIONS_READ] = count
        }
    }

    fun getAutoBoardService(): Flow<Boolean> = dataStore.data.map {
        it[AUTO_BOARD_SERVICE] ?: false
    }

    suspend fun saveAutoBoardService(autoBoardServiceState: Boolean) {
        dataStore.edit {
            it[AUTO_BOARD_SERVICE] = autoBoardServiceState
        }
    }

    fun getColorBlindMode(): Flow<Boolean> = dataStore.data.map {
        it[COLOR_BLIND_MODE] ?: false
    }

    suspend fun saveColorBlindMode(colorBlindMode: Boolean) {
        dataStore.edit {
            it[COLOR_BLIND_MODE] = colorBlindMode
        }
    }

    fun getPrivacyPolicyAccepted(): Flow<Boolean> = dataStore.data.map {
        it[PRIVACY_POLICY_ACCEPTED] ?: false
    }

    suspend fun savePrivacyPolicyAccepted(privacyPolicyAccepted: Boolean) {
        dataStore.edit {
            it[PRIVACY_POLICY_ACCEPTED] = privacyPolicyAccepted
        }
    }

    fun getAboutAccepted(): Flow<Boolean> = dataStore.data.map {
        it[ABOUT_ACCEPTED] ?: false
    }

    suspend fun saveAboutAccepted(aboutAccepted: Boolean) {
        dataStore.edit {
            it[ABOUT_ACCEPTED] = aboutAccepted
        }
    }

    fun getMaxStopDist(): Flow<Float> = dataStore.data.map {
        it[MAX_STOP_DIST] ?: 20f
    }

    suspend fun saveMaxStopDist(minStopDist: Float) {
        dataStore.edit {
            it[MAX_STOP_DIST] = minStopDist
        }
    }

    fun getBaseUrl(): Flow<String> = dataStore.data.map {
        it[BASE_URL] ?: context.getString(R.string.url_default)
    }

    suspend fun saveBaseUrl(url: String) {
        dataStore.edit {
            it[BASE_URL] = url
        }
    }

    suspend fun getBoardBusCount(): Int = dataStore.data.map {
        it[BOARD_BUS_COUNT] ?: 0
    }.first()

    suspend fun incrementBoardBusCount() {
        dataStore.edit {
            it[BOARD_BUS_COUNT] = getBoardBusCount() + 1
        }
    }

    fun getAllowAnalytics(): Flow<Boolean> = dataStore.data.map {
        it[ALLOW_ANALYTICS] ?: false
    }

    suspend fun saveAllowAnalytics(allowAnalytics: Boolean) {
        dataStore.edit {
            it[ALLOW_ANALYTICS] = allowAnalytics
        }
    }
}
