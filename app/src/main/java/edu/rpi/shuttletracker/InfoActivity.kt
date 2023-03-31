package edu.rpi.shuttletracker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InfoActivity : AppCompatActivity() {

    private val weekArray = arrayOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        val toolbar: Toolbar = findViewById(R.id.infoToolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setDisplayShowHomeEnabled(true)

        val scheduleText: TextView = findViewById(R.id.infoScheduleTextView)
        scheduleText.text = getSchedule()

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getSchedule(): String {
        var scheduleString = ""
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val server_url = sharedPreferences.getString("server_base_url", resources.getString(R.string.default_server_url))
        val sched = URL(server_url + resources.getString(R.string.schedule_url))
        val thread = Thread {
            kotlin.run{
                try {
                    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
                    Log.d("dynamic scheduling", "current date: $currentDate")
                    val scheduleArray = JSONArray(sched.readText())
                    (0 until scheduleArray.length()).forEach {
                        val semester = scheduleArray.getJSONObject(it)
//                        Log.d("dynamic scheduling", semester.toString() + " break ")

                        val start = semester.getString("start")
                        val end = semester.getString("end")
                        val startDate = LocalDateTime.parse(start, format)
                        val endDate = LocalDateTime.parse(end, format)

//                        Log.d("dynamic scheduling", "start date: $startDate")
//                        Log.d("dynamic scheduling", "end date: $endDate")
//
//                        Log.d("dynamic scheduling", "current date is within bounds: ${currentDate.isAfter(startDate) && currentDate.isBefore(endDate)}")

                        if(currentDate.isAfter(startDate) && currentDate.isBefore(endDate)){
                            val content = semester.getJSONObject("content")
//                            Log.d("dynamic scheduling", "content: $content")

                            scheduleString = formateSchedule(content)
                        }
                    }
                } catch (ex: Exception) {
                    Logs.writeExceptionToLogBuffer(object{}.javaClass.enclosingMethod.name, ex)
                    Logs.sendLogsToServer(getLogsURL())
                }
            }
        }
        thread.start()
        thread.join()

        Log.d("dynamic scheduling", scheduleString)
        return scheduleString
    }

    private fun formateSchedule(content: JSONObject): String {

        val scheduleString = StringBuilder()

        for (i in weekArray.indices){
//            Log.d("dynamic scheduling", "${content.getJSONObject(weekArray[i])}")
            val daySchedule = content.getJSONObject(weekArray[i])
            scheduleString.append(daySchedule.getString("start"))
            scheduleString.append(" to ")
            scheduleString.appendLine(daySchedule.getString("end"))
        }

        return scheduleString.toString()
    }

    // for future logging
    private fun getLogsURL(): URL {
        val res : Resources = getResources()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val server_url = sharedPreferences.getString("server_base_url", res.getString(R.string.default_server_url))
        val logsUrl =
            URL(server_url + res.getString(R.string.logs_url))
        return logsUrl
    }
}