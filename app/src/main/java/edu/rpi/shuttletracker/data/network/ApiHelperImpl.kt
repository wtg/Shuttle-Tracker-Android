package edu.rpi.shuttletracker.data.network

import com.google.gson.JsonObject
import com.haroldadmin.cnradapter.NetworkResponse
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Coordinate
import edu.rpi.shuttletracker.data.models.ErrorResponse
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ApiHelperImpl
    @Inject
    constructor(private val apiService: ApiService) : ApiHelper {
        override suspend fun getRunningBuses(): Flow<NetworkResponse<Map<String, Bus>, ErrorResponse>> =
            flow {
                while (true) {
                    emit(apiService.getRunningBuses())
                    delay(5000)
                }
            }

        override suspend fun getAllBuses(): Flow<NetworkResponse<Map<String, Bus>, ErrorResponse>> =
            flow {
                while (true) {
                    emit(apiService.getAllBuses())
                    delay(5000)
                }
            }

        override suspend fun getStops(): NetworkResponse<List<Stop>, ErrorResponse> {
            return when (val resp = apiService.getRoutesRaw()) {
                is NetworkResponse.Success -> {
                    val stops = parseStopsFromSchema(resp.body)
                    NetworkResponse.Success(stops, resp.response)
                }
                is NetworkResponse.ServerError -> NetworkResponse.ServerError(resp.body, resp.response)
                is NetworkResponse.NetworkError -> NetworkResponse.NetworkError(resp.error)
                is NetworkResponse.UnknownError -> NetworkResponse.UnknownError(resp.error, resp.response)
            }
        }

        private fun parseStopsFromSchema(root: JsonObject): List<Stop> {
            val allStops = mutableListOf<Stop>()

            for ((routeName, routeElem) in root.entrySet()) {
                val routeObj = routeElem.asJsonObject
                val stopIds = routeObj.getAsJsonArray("STOPS") ?: continue

                for (stopIdElement in stopIds) {
                    val stopId = stopIdElement.asString
                    val stopObj = routeObj.getAsJsonObject(stopId) ?: continue
                    val coords = stopObj.getAsJsonArray("COORDINATES") ?: continue
                    if (coords.size() < 2) continue

                    val latitude = coords[0].asDouble
                    val longitude = coords[1].asDouble
                    val name = stopObj.get("NAME")?.asString ?: stopId
                    val offset = stopObj.get("OFFSET").asInt

                    allStops.add(Stop(latitude, longitude, name, offset, routeName))
                }
            }

            return allStops
        }

        override suspend fun getRoutes(): NetworkResponse<List<Route>, ErrorResponse> {
            return when (val resp = apiService.getRoutesRaw()) {
                is NetworkResponse.Success -> {
                    val stops = parseRoutesFromSchema(resp.body)
                    NetworkResponse.Success(stops, resp.response)
                }
                is NetworkResponse.ServerError -> NetworkResponse.ServerError(resp.body, resp.response)
                is NetworkResponse.NetworkError -> NetworkResponse.NetworkError(resp.error)
                is NetworkResponse.UnknownError -> NetworkResponse.UnknownError(resp.error, resp.response)
            }
        }

        private fun parseRoutesFromSchema(root: JsonObject): List<Route> {
            val excludedRoutes = setOf("ENTRY1", "EXIT1", "EXIT2")
            val allRoutes = mutableListOf<Route>()

            for ((routeName, routeElem) in root.entrySet()) {
                if (routeName in excludedRoutes) continue

                val routeObj = routeElem.asJsonObject

                val color = routeObj.get("COLOR")?.asString ?: continue
                val routesArray = routeObj.getAsJsonArray("ROUTES") ?: continue

                val coordinates = mutableListOf<Coordinate>()

                for (polylineElem in routesArray) {
                    val polyline = polylineElem.asJsonArray

                    for (coordElem in polyline) {
                        val coords = coordElem.asJsonArray
                        if (coords.size() >= 2) {
                            val latitude = coords[0].asDouble
                            val longitude = coords[1].asDouble
                            coordinates.add(Coordinate(latitude, longitude))
                        }
                    }
                }
                allRoutes.add(
                    Route(colorName = color, coordinates = coordinates),
                )
            }
            return allRoutes
        }

        override suspend fun addBus(
            busNum: Int,
            bus: BoardBus,
        ): NetworkResponse<Unit, ErrorResponse> = apiService.addBus(busNum, bus)

        override suspend fun getAnnouncements(): NetworkResponse<List<Announcement>, ErrorResponse> =
            apiService.getAnnouncements()

        override suspend fun getSchedule(): NetworkResponse<List<Schedule>, ErrorResponse> = apiService.getSchedule()

        override suspend fun addAnalytics(analytics: Analytics): NetworkResponse<Unit, ErrorResponse> =
            apiService.addAnalytics(
                analytics,
            )

        override suspend fun sendRegistrationToken(token: String): NetworkResponse<Unit, ErrorResponse> =
            apiService.sendRegistrationToken(token)
    }
