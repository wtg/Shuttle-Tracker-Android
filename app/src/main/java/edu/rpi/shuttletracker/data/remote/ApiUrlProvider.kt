package edu.rpi.shuttletracker.data.remote

import edu.rpi.shuttletracker.core.network.normalizeBaseUrl
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.first

fun interface ApiUrlProvider {
    suspend fun endpoint(path: String): String
}

/** Resolves API endpoints without blocking the main thread while DataStore loads. */
class DataStoreApiUrlProvider(
    private val userPreferences: UserPreferences,
    defaultBaseUrl: String,
) : ApiUrlProvider {
    val defaultBaseUrl =
        checkNotNull(normalizeBaseUrl(defaultBaseUrl)) {
            "The default API URL must be a valid HTTP(S) base URL"
        }

    override suspend fun endpoint(path: String): String {
        val baseUrl = normalizeBaseUrl(userPreferences.getBaseUrl().first()) ?: defaultBaseUrl
        return baseUrl + path.trimStart('/')
    }
}
