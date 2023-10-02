package edu.rpi.shuttletracker.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    }
    fun getNotificationsRead(): Flow<Int> = dataStore.data.map {
        it[NOTIFICATIONS_READ] ?: 0
    }

    suspend fun saveNotificationsRead(count: Int) {
        dataStore.edit {
            it[NOTIFICATIONS_READ] = count
        }
    }
}
