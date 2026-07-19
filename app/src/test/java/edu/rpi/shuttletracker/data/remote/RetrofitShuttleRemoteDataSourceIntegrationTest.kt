package edu.rpi.shuttletracker.data.remote

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.core.network.NetworkError
import edu.rpi.shuttletracker.core.network.NetworkResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RetrofitShuttleRemoteDataSourceIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: RetrofitShuttleRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        val api = retrofit.create(ShuttleApi::class.java)
        dataSource = RetrofitShuttleRemoteDataSource(api, retrofit)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `routes endpoint parses realistic dynamic stop response`() =
        runTest {
            server.enqueue(jsonResponse(resource("routes_success.json")))

            val result = dataSource.getRoutes()

            assertThat(server.takeRequest().path).isEqualTo("/routes")
            val routes = (result as NetworkResult.Success).data
            assertThat(routes.getValue("NORTH").stopDetails).containsKey("union")
        }

    @Test
    fun `malformed route response becomes unknown failure`() =
        runTest {
            server.enqueue(jsonResponse(resource("routes_malformed.json")))

            val result = dataSource.getRoutes()

            assertThat(result).isInstanceOf(NetworkResult.Failure::class.java)
            assertThat((result as NetworkResult.Failure).error).isInstanceOf(NetworkError.Unknown::class.java)
        }

    @Test
    fun `empty announcement response remains successful`() =
        runTest {
            server.enqueue(jsonResponse("""{"announcements":[]}"""))

            val result = dataSource.getAnnouncements()

            assertThat(server.takeRequest().path).isEqualTo("/announcements")
            assertThat((result as NetworkResult.Success).data).isEmpty()
        }

    @Test
    fun `announcement response unwraps the wrapper object`() =
        runTest {
            server.enqueue(
                jsonResponse(
                    """
                    {
                      "announcements": [
                        {"id": "snow-delay", "message": "Delays expected", "type": "warning", "active": true}
                      ]
                    }
                    """.trimIndent(),
                ),
            )

            val result = dataSource.getAnnouncements()

            val announcements = (result as NetworkResult.Success).data
            assertThat(announcements.single().id).isEqualTo("snow-delay")
        }

    @Test
    fun `HTTP error preserves status and backend reason`() =
        runTest {
            server.enqueue(jsonResponse("""{"error":true,"reason":"maintenance"}""", code = 503))

            val result = dataSource.getAnnouncements()

            val error = (result as NetworkResult.Failure).error as NetworkError.Http
            assertThat(error.statusCode).isEqualTo(503)
            assertThat(error.message).isEqualTo("maintenance")
        }

    @Test
    fun `locations endpoint maps backend field names`() =
        runTest {
            server.enqueue(jsonResponse(resource("locations_success.json")))

            val result = dataSource.getVehicleLocations()

            assertThat(server.takeRequest().path).isEqualTo("/locations")
            assertThat((result as NetworkResult.Success).data.getValue("bus-1").speedMph).isEqualTo(12.0)
        }

    @Test
    fun `schedule endpoint maps the complete weekly response`() =
        runTest {
            server.enqueue(jsonResponse(resource("schedule_success.json")))

            val result = dataSource.getSchedule()

            assertThat(server.takeRequest().path).isEqualTo("/schedule")
            assertThat((result as NetworkResult.Success).data.weekday).containsKey("North Bus")
        }

    @Test
    fun `ETA and velocity calls use their declared endpoint paths`() =
        runTest {
            server.enqueue(jsonResponse("{}"))
            server.enqueue(jsonResponse("{}"))

            dataSource.getVehicleEtas()
            dataSource.getVehicleVelocities()

            assertThat(server.takeRequest().path).isEqualTo("/etas")
            assertThat(server.takeRequest().path).isEqualTo("/velocities")
        }

    private fun resource(name: String): String =
        checkNotNull(javaClass.getResource("/api/$name")) { "Missing fixture $name" }.readText()

    private fun jsonResponse(
        body: String,
        code: Int = 200,
    ) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
