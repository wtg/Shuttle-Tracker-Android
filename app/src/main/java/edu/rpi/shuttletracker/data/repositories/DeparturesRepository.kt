package edu.rpi.shuttletracker.data.repositories

import android.app.AlarmManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.rpi.shuttletracker.data.database.daos.DepartureDao
import edu.rpi.shuttletracker.data.models.Departure
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeparturesRepository
    @Inject
    constructor(
        private val departureDao: DepartureDao,
        @ApplicationContext private val context: Context,
    ) {
        fun getAllDepartures(): Flow<List<Departure>> = departureDao.getAllDepartures()

        fun getDepartures(name: String): Flow<List<Departure>> = departureDao.getDepartures(name)

        suspend fun insertDeparture(departure: Departure) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // removes all the alarms for the id
            for (intent in departureDao.getDeparture(departure.id)?.getAlarmIntent(context) ?: listOf()) {
                alarmMgr.cancel(intent)
            }

            // ensures we don't modify the old departure
            val newDeparture = departure.copy(id = departureDao.insertDeparture(departure).toInt())

            newDeparture.getAlarmIntent(context).zip(
                departure.getMillis(),
            ) { intent, millis ->
                alarmMgr.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    millis,
                    AlarmManager.INTERVAL_DAY * 7,
                    intent,
                )
            }
        }

        suspend fun deleteDeparture(departure: Departure) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // removes all the alarms for the id
            for (intent in departure.getAlarmIntent(context)) {
                alarmMgr.cancel(intent)
            }

            departureDao.deleteDeparture(departure)
        }
    }
