package edu.rpi.shuttletracker.data.remote.dto

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import org.junit.Test

class RouteDtoDeserializerTest {
    private val gson =
        GsonBuilder()
            .registerTypeAdapter(RouteDto::class.java, RouteDtoDeserializer())
            .create()

    @Test
    fun `reads stop details from dynamic route keys`() {
        val route = gson.fromJson(validRouteJson(), RouteDto::class.java)

        assertThat(route.stopDetails.keys).containsExactly("union", "academy")
        assertThat(route.stopDetails.getValue("union").name).isEqualTo("Student Union")
    }

    @Test
    fun `ignores extra fields that are not listed route stops`() {
        val json = validRouteJson().replace("\"academy\":", "\"metadata\":{\"value\":1},\"academy\":")

        val route = gson.fromJson(json, RouteDto::class.java)

        assertThat(route.stopDetails).doesNotContainKey("metadata")
    }

    @Test
    fun `malformed optional stop detail is skipped`() {
        val json = validRouteJson().replace("\"COORDINATES\":[42.731,-73.679]", "\"COORDINATES\":\"invalid\"")

        val route = gson.fromJson(json, RouteDto::class.java)

        assertThat(route.stopDetails).containsKey("union")
        assertThat(route.stopDetails).doesNotContainKey("academy")
    }

    private fun validRouteJson() =
        """
        {
          "COLOR":"#D32F2F",
          "STOPS":["union","academy"],
          "POLYLINE_STOPS":[],
          "ROUTES":[[[42.730,-73.680],[42.731,-73.679]]],
          "union":{"COORDINATES":[42.730,-73.680],"OFFSET":0,"NAME":"Student Union"},
          "academy":{"COORDINATES":[42.731,-73.679],"OFFSET":5,"NAME":"Academy Hall"}
        }
        """.trimIndent()
}
