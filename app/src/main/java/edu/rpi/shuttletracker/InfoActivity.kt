package edu.rpi.shuttletracker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InfoActivity : AppCompatActivity() {

    private val WEEK_ARRAY =
        arrayOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        val toolbar: Toolbar = findViewById(R.id.infoToolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setDisplayShowHomeEnabled(true)

        val scheduleText: TextView = findViewById(R.id.infoScheduleTextView)
        val dayScheduleText: TextView = findViewById(R.id.daySchedule)
        val formattedSchedule = getSchedule()

        if (formattedSchedule != null) {
            scheduleText.text = formattedSchedule
        } else {
            Logs.writeToLogBuffer(
                object {}.javaClass.enclosingMethod.name,
                "no schedule found, displaying default schedule message",
            )
            dayScheduleText.visibility = View.GONE
            val param = scheduleText.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20, 0, 20)
            dayScheduleText.layoutParams = param
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getSchedule(): String? {
        val res: Resources = getResources()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        var scheduleString: String? = null
        val server_url = sharedPreferences.getString(
            "server_base_url",
            resources.getString(R.string.default_server_url),
        )
        val sched = URL(server_url + resources.getString(R.string.schedule_url))
        val thread = Thread {
            kotlin.run {
                try {
                    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
                    val scheduleArray = JSONArray(sched.readText())
                    Logs.writeToLogBuffer(
                        object {}.javaClass.enclosingMethod.name,
                        "retrieved JSON schedule from server: $scheduleArray",
                    )
                    (0 until scheduleArray.length()).forEach {
                        val semester = scheduleArray.getJSONObject(it)

                        val start = semester.getString("start")
                        val end = semester.getString("end")
                        val startDate = LocalDateTime.parse(start, format)
                        val endDate = LocalDateTime.parse(end, format)

                        if (currentDate.isAfter(startDate) && currentDate.isBefore(endDate)) {
                            Logs.writeToLogBuffer(
                                object {}.javaClass.enclosingMethod.name,
                                "current date $currentDate is between $start and $end",
                            )
                            val content = semester.getJSONObject("content")
                            scheduleString = formatSchedule(content)
                        }
                    }
                } catch (ex: Exception) {
                    Logs.writeExceptionToLogBuffer(object {}.javaClass.enclosingMethod.name, ex)
                    Logs.sendLogsToServer(getLogsURL())
                    scheduleString = null
                }
            }
        }
        thread.start()
        thread.join()

        return scheduleString
    }

    private fun formatSchedule(content: JSONObject): String? {
        val scheduleString = StringBuilder()

        for (i in WEEK_ARRAY.indices) {
            try {
                val daySchedule = content.getJSONObject(WEEK_ARRAY[i])
                scheduleString.append(daySchedule.getString("start"))
                scheduleString.append(" to ")
                scheduleString.appendLine(daySchedule.getString("end"))
            } catch (ex: Exception) {
                Logs.writeToLogBuffer(
                    object {}.javaClass.enclosingMethod.name,
                    "JSON content not in form of Monday, Tuesday,...",
                )
                Logs.writeExceptionToLogBuffer(object {}.javaClass.enclosingMethod.name, ex)
                Logs.sendLogsToServer(getLogsURL())
                return null
            }
        }

        return scheduleString.dropLast(1).toString()
    }

    private fun getLogsURL(): URL {
        val res: Resources = getResources()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val server_url = sharedPreferences.getString(
            "server_base_url",
            res.getString(R.string.default_server_url),
        )
        val logsUrl =
            URL(server_url + res.getString(R.string.logs_url))
        return logsUrl
    }
}
