package edu.rpi.shuttletracker.data.models

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.testing.fixtures.testVehicleEta
import edu.rpi.shuttletracker.testing.fixtures.testVehicleLocation
import edu.rpi.shuttletracker.testing.fixtures.testVehicleVelocity
import org.junit.Test

class VehicleMergerTest {
    @Test
    fun `merges location velocity and eta with the same vehicle id`() {
        val result =
            VehicleMerger
                .merge(
                    locations = mapOf("bus-1" to testVehicleLocation()),
                    velocities = mapOf("bus-1" to testVehicleVelocity()),
                    etas = mapOf("bus-1" to testVehicleEta()),
                ).single()

        assertThat(result.id).isEqualTo("bus-1")
        assertThat(result.routeName).isEqualTo("NORTH")
        assertThat(result.stopTimes).containsEntry("union", "2 min")
    }

    @Test
    fun `missing optional responses keep the location vehicle`() {
        val result = VehicleMerger.merge(mapOf("bus-1" to testVehicleLocation())).single()

        assertThat(result.routeName).isNull()
        assertThat(result.isAtStop).isNull()
        assertThat(result.stopTimes).isEmpty()
    }

    @Test
    fun `responses without a matching location are ignored`() {
        val result =
            VehicleMerger.merge(
                locations = emptyMap(),
                velocities = mapOf("bus-1" to testVehicleVelocity()),
                etas = mapOf("bus-1" to testVehicleEta()),
            )

        assertThat(result).isEmpty()
    }

    @Test
    fun `vehicles are sorted by display name`() {
        val result =
            VehicleMerger.merge(
                mapOf(
                    "bus-2" to testVehicleLocation("West Bus"),
                    "bus-1" to testVehicleLocation("North Bus"),
                ),
            )

        assertThat(result.map { it.name }).containsExactly("North Bus", "West Bus").inOrder()
    }
}
