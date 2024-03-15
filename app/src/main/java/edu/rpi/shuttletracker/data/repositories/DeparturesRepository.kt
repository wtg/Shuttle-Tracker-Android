package edu.rpi.shuttletracker.data.repositories

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.rpi.shuttletracker.data.database.daos.DepartureDao
import edu.rpi.shuttletracker.data.models.Departure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

class DeparturesRepository
    @Inject
    constructor(
        private val departureDao: DepartureDao,
        @ApplicationContext private val context: Context,
    ) {
        fun getAllDepartures(): Flow<List<Departure>> = departureDao.getAllDepartures()

        fun getAllDeparturesGrouped(): Flow<List<List<Departure>>> =
            departureDao.getAllDeparturesGrouped()
                .transform { departures ->

                    emit(departures.groupBy { it.stop }.values.toList())
                }

        fun getDepartures(name: String): Flow<List<Departure>> = departureDao.getDepartures(name)

        suspend fun insertDeparture(departure: Departure) {
            // removes all the alarms for the id
            departureDao.getDeparture(departure.id)?.cancelAlarms(context)

            // ensures we don't modify the old departure
            val newDeparture = departure.copy(id = departureDao.insertDeparture(departure).toInt())

            newDeparture.initiateAlarms(context)
        }

        suspend fun deleteDeparture(departure: Departure) {
            departure.cancelAlarms(context)
            departureDao.deleteDeparture(departure)
        }
    }
