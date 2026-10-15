package edu.rpi.shuttletracker.feature.map

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import org.junit.Test

class MapGeometryTest {
    @Test
    fun `backend latitude first coordinates become GeoJSON longitude first`() {
        val position = Stop(listOf(42.73, -73.68), 0, "Union").mapPosition()

        assertThat(position.longitude).isEqualTo(-73.68)
        assertThat(position.latitude).isEqualTo(42.73)
    }

    @Test
    fun `route gaps remain separate and incomplete segments are omitted`() {
        val route =
            testRoute().copy(
                coordinates =
                    listOf(
                        listOf(listOf(42.73, -73.68), listOf(42.74, -73.67)),
                        listOf(listOf(42.75, -73.66), listOf(42.76, -73.65)),
                        listOf(listOf(42.77, -73.64)),
                        listOf(listOf(42.78), listOf(Double.NaN, -73.63)),
                    ),
            )

        val segments = route.mapGeometry().coordinates

        assertThat(segments).hasSize(2)
        assertThat(segments[0].first().longitude).isEqualTo(-73.68)
        assertThat(segments[1].first().latitude).isEqualTo(42.75)
        assertThat(segments.map { it.size }).containsExactly(2, 2)
    }
}
