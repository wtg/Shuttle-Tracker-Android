package edu.rpi.shuttletracker.data.repositories

import edu.rpi.shuttletracker.data.database.daos.DepartureDao
import edu.rpi.shuttletracker.data.models.Departure
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeparturesRepository
    @Inject
    constructor(
        private val departureDao: DepartureDao,
    ) {
        fun getAllDepartures(): Flow<List<Departure>> = departureDao.getAllDepartures()

        fun getDepartures(name: String): Flow<List<Departure>> = departureDao.getDepartures(name)

        suspend fun insertDeparture(departure: Departure) {
            departureDao.insertDeparture(departure)
        }

        suspend fun deleteDeparture(departure: Departure) {
            departureDao.deleteDeparture(departure)
        }
    }
