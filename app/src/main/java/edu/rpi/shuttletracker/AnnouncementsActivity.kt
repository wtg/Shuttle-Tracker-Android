package edu.rpi.shuttletracker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AnnouncementsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcements)
        val thread = Thread(
            kotlinx.coroutines.Runnable {
                kotlin.run {
                    try {
                        val sharedPreferences: SharedPreferences =
                            getSharedPreferences("preferences", Context.MODE_PRIVATE)
                        val server_url = sharedPreferences.getString(
                            "server_base_url",
                            resources.getString(R.string.default_server_url),
                        )
                        val announ =
                            URL(server_url + resources.getString(R.string.announcements_url))
                        var announString = announ.readText()
                        val announArray = JSONArray(announString)
                        val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
                        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

                        announString = ""
                        for (i in 0 until announArray.length()) {
                            val sched_type = announArray.getJSONObject(i).getString("scheduleType")
                            val start = announArray.getJSONObject(i).getString("start")
                            val end = announArray.getJSONObject(i).getString("end")
                            val startDate = LocalDateTime.parse(start, format)
                            val endDate = LocalDateTime.parse(end, format)
                            if ((currentDate >= startDate && currentDate <= endDate) || (sched_type == "none") ||
                                (sched_type == "startOnly" && currentDate >= startDate) ||
                                (sched_type == "endOnly" && currentDate <= endDate)
                            ) {
                                announString += startDate.toString().substringBefore('T') + ":\n" +
                                    "____________" + "\n" +
                                    announArray.getJSONObject(i)
                                        .getString("body") + "\n" + "\n\n\n"
                            }
                        }
                        if (announString.length > 0) {
                            findViewById<TextView>(R.id.announcementsTextView).text = announString
                        }
                    } catch (ex: Exception) {
                        Logs.writeExceptionToLogBuffer(object {}.javaClass.enclosingMethod.name, ex)
                        Logs.sendLogsToServer(getLogsURL())
                    }
                }
            },
        )
        thread.start()
        thread.join()
        val toolbar: Toolbar = findViewById(R.id.announcementsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.itemId == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
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
