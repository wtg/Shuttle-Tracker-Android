package edu.rpi.shuttletracker.data.local.preferences

import com.google.maps.android.compose.MapType
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Reads and saves the user's app settings.
 *
 * The interface lets tests replace DataStore with an in-memory fake.
 */
interface UserPreferences {
    fun getMapType(): Flow<MapType>

    suspend fun saveMapType(mapType: MapType)

    fun getPrivacyPolicyAccepted(): Flow<Boolean>

    suspend fun savePrivacyPolicyAccepted(privacyPolicyAccepted: Boolean)

    fun getAboutAccepted(): Flow<Boolean>

    suspend fun saveAboutAccepted(aboutAccepted: Boolean)

    fun getSetupCompleted(): Flow<Boolean>

    suspend fun saveSetupCompleted(setupCompleted: Boolean)

    suspend fun activateDevOptions(devOptionEnable: Boolean)

    fun getDevOptions(): Flow<Boolean>

    fun getSimulateAnnouncements(): Flow<Boolean>

    suspend fun saveSimulateAnnouncements(enabled: Boolean)

    fun getThemeMode(): Flow<ThemeMode>

    suspend fun saveThemeMode(themeMode: ThemeMode)

    suspend fun resetSetup()

    fun getShuttleAnimations(): Flow<Boolean>

    suspend fun saveShuttleAnimations(animationsEnable: Boolean)

    fun getShuttleRotation(): Flow<Boolean>

    suspend fun saveShuttleRotations(rotationsEnable: Boolean)
}
