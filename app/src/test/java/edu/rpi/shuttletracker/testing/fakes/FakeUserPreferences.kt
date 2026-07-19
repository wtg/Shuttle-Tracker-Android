package edu.rpi.shuttletracker.testing.fakes

import com.google.maps.android.compose.MapType
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferences : UserPreferences {
    val notificationsRead = MutableStateFlow(0)
    val mapType = MutableStateFlow(MapType.NORMAL)
    val privacyPolicyAccepted = MutableStateFlow(false)
    val aboutAccepted = MutableStateFlow(false)
    val setupCompleted = MutableStateFlow(false)
    val baseUrl = MutableStateFlow("https://example.com/")
    val devOptions = MutableStateFlow(false)
    val themeMode = MutableStateFlow(ThemeMode.System)
    val shuttleAnimations = MutableStateFlow(false)
    val shuttleRotation = MutableStateFlow(true)

    var saveNotificationsReadCalls = 0
    var saveAboutAcceptedCalls = 0
    var savePrivacyPolicyAcceptedCalls = 0
    var saveSetupCompletedCalls = 0
    var aboutSaveGate: CompletableDeferred<Unit>? = null

    override fun getNotificationsRead(): Flow<Int> = notificationsRead

    override suspend fun saveNotificationsRead(count: Int) {
        saveNotificationsReadCalls++
        notificationsRead.value = count
    }

    override fun getMapType(): Flow<MapType> = mapType

    override suspend fun saveMapType(mapType: MapType) {
        this.mapType.value = mapType
    }

    override fun getPrivacyPolicyAccepted(): Flow<Boolean> = privacyPolicyAccepted

    override suspend fun savePrivacyPolicyAccepted(privacyPolicyAccepted: Boolean) {
        savePrivacyPolicyAcceptedCalls++
        this.privacyPolicyAccepted.value = privacyPolicyAccepted
    }

    override fun getAboutAccepted(): Flow<Boolean> = aboutAccepted

    override suspend fun saveAboutAccepted(aboutAccepted: Boolean) {
        saveAboutAcceptedCalls++
        aboutSaveGate?.await()
        this.aboutAccepted.value = aboutAccepted
    }

    override fun getSetupCompleted(): Flow<Boolean> = setupCompleted

    override suspend fun saveSetupCompleted(setupCompleted: Boolean) {
        saveSetupCompletedCalls++
        this.setupCompleted.value = setupCompleted
    }

    override fun getBaseUrl(): Flow<String> = baseUrl

    override suspend fun saveBaseUrl(url: String) {
        baseUrl.value = url
    }

    override suspend fun activateDevOptions(devOptionEnable: Boolean) {
        devOptions.value = devOptionEnable
    }

    override fun getDevOptions(): Flow<Boolean> = devOptions

    override fun getThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun saveThemeMode(themeMode: ThemeMode) {
        this.themeMode.value = themeMode
    }

    override suspend fun resetSetup() {
        aboutAccepted.value = false
        privacyPolicyAccepted.value = false
        setupCompleted.value = false
    }

    override fun getShuttleAnimations(): Flow<Boolean> = shuttleAnimations

    override suspend fun saveShuttleAnimations(animationsEnable: Boolean) {
        shuttleAnimations.value = animationsEnable
    }

    override fun getShuttleRotation(): Flow<Boolean> = shuttleRotation

    override suspend fun saveShuttleRotations(rotationsEnable: Boolean) {
        shuttleRotation.value = rotationsEnable
    }
}
