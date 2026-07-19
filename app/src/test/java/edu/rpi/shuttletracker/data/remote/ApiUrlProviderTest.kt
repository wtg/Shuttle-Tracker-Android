package edu.rpi.shuttletracker.data.remote

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ApiUrlProviderTest {
    private val preferences = FakeUserPreferences()

    @Test
    fun `uses the stored base URL without blocking construction`() =
        runTest {
            preferences.baseUrl.value = "https://developer.example/api"
            val provider = DataStoreApiUrlProvider(preferences, "https://default.example/api/")

            assertThat(provider.endpoint("routes"))
                .isEqualTo("https://developer.example/api/routes")
        }

    @Test
    fun `falls back to the default when the stored URL is invalid`() =
        runTest {
            preferences.baseUrl.value = "not a URL"
            val provider = DataStoreApiUrlProvider(preferences, "https://default.example/api")

            assertThat(provider.endpoint("/schedule"))
                .isEqualTo("https://default.example/api/schedule")
        }
}
