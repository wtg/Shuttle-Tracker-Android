package edu.rpi.shuttletracker.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val NOTIFICATIONS_READ = intPreferencesKey("notifications_read")
        private val AUTO_BOARD_SERVICE = booleanPreferencesKey("auto_board_service")
        private val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
        private val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        private val ABOUT_ACCEPTED = booleanPreferencesKey("about_accepted")
    }

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

    suspend fun savePrivacyPolicyAccepted(privacyPollicyAccepted: Boolean) {
        dataStore.edit {
            it[PRIVACY_POLICY_ACCEPTED] = privacyPollicyAccepted
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
}
