package edu.rpi.shuttletracker.data.models

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.rpi.shuttletracker.util.alarms.AlarmReceiver
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar

@Entity(tableName = "departures")
data class Departure(
    @ColumnInfo(name = "stop_name")
    val stop: String,
    // based on Calendar class, 1 is saturday, 7 is sunday
    @ColumnInfo(name = "days_active")
    var days: List<Int>,
    @ColumnInfo(name = "time")
    var time: String = LocalDateTime.now().toString(),
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {
    fun getReadableTime(): String {
        val outputFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

        val busDate = LocalDateTime.parse(time)

        return busDate.format(outputFormatter)
    }

    private fun getMinute(): Int = LocalDateTime.parse(time).minute

    private fun getHour(): Int = LocalDateTime.parse(time).hour

    fun setTime(
        hour: Int,
        minute: Int,
    ) {
        time = LocalDateTime.now().with(LocalTime.of(hour, minute)).toString()
    }

    private fun getAlarmIntent(context: Context): List<PendingIntent> =
        days.map { day ->
            val requestCode = id * 7 + (day - 1)
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("stop", stop)
                putExtra("id", requestCode)
            }.let { intent ->
                // request code: spaced by 7 and day - 1 puts it in the specific spot
                PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
            }
        }

    private fun getMillis(): List<Long> =
        days.map { day ->
            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()

                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MINUTE, getMinute())
                    set(Calendar.HOUR_OF_DAY, getHour())
                    set(Calendar.DAY_OF_WEEK, day)

                    if (timeInMillis < System.currentTimeMillis()) {
                        timeInMillis += 24 * 60 * 60 * 1000 * 7 // Next week
                    }
                }

            return@map calendar.timeInMillis
        }

    fun cancelAlarms(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // removes all the alarms for the id
        for (intent in getAlarmIntent(context)) {
            alarmMgr.cancel(intent)
        }
    }

    fun initiateAlarms(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        getAlarmIntent(context).zip(
            getMillis(),
        ) { intent, millis ->
            alarmMgr.setRepeating(
                AlarmManager.RTC_WAKEUP,
                millis,
                AlarmManager.INTERVAL_DAY * 7,
                intent,
            )
        }
    }
}
