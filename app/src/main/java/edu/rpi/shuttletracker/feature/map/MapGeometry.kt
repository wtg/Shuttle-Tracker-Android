package edu.rpi.shuttletracker.feature.map

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import org.maplibre.spatialk.geojson.MultiLineString
import org.maplibre.spatialk.geojson.Position

internal fun Stop.mapPosition(): Position = Position(longitude = coordinates[1], latitude = coordinates[0])

internal fun Vehicle.mapPosition(): Position = Position(longitude = longitude, latitude = latitude)

internal fun Route.mapGeometry(): MultiLineString =
    MultiLineString(
        coordinates.mapNotNull { segment ->
            segment
                .filter { it.size >= 2 && it[0].isFinite() && it[1].isFinite() }
                .map { Position(longitude = it[1], latitude = it[0]) }
                .takeIf { it.size >= 2 }
        },
    )
