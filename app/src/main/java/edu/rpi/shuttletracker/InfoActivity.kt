package edu.rpi.shuttletracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray
import java.net.URL

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        val toolbar: Toolbar = findViewById(R.id.infoToolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setDisplayShowHomeEnabled(true)
        val semesterArray = ArrayList<Schedule>()

        val thread = Thread(kotlinx.coroutines.Runnable {
            kotlin.run {
                val url = URL("https://shuttletracker.app/schedule")
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {

                    val semSchedule = jsonArray.getJSONObject(i)
                    val name = semSchedule.getString("name")
                    val start = semSchedule.getString("start")
                    val end = semSchedule.getString("end")
                    val content = semSchedule.getJSONObject("content")
                    val dayArray = ArrayList<Day>()

                    //monday
                    val monday = content.getJSONObject("monday")
                    val dayStart = monday.getString("start")
                    val dayEnd = monday.getString("end")
                    val dayObject = Day("Monday", dayStart, dayEnd)
                    dayArray.add(dayObject)

                    //tuesday
                    val tuesday = content.getJSONObject("tuesday")
                    val tuesdayStart = tuesday.getString("start")
                    val tuesdayEnd = tuesday.getString("end")
                    val tuesdayObject = Day("Tuesday", tuesdayStart, tuesdayEnd)
                    dayArray.add(tuesdayObject)
                    //wednesday
                    val wednesday = content.getJSONObject("wednesday")
                    val wednesdayStart = wednesday.getString("start")
                    val wednesdayEnd = wednesday.getString("end")
                    val wednesdayObject = Day("Wednesday", wednesdayStart, wednesdayEnd)
                    dayArray.add(wednesdayObject)
                    //thursday
                    val thursday = content.getJSONObject("thursday")
                    val thursdayStart = thursday.getString("start")
                    val thursdayEnd = thursday.getString("end")
                    val thursdayObject = Day("Thursday", thursdayStart, thursdayEnd)
                    dayArray.add(thursdayObject)
                    //friday
                    val friday = content.getJSONObject("friday")
                    val fridayStart = friday.getString("start")
                    val fridayEnd = friday.getString("end")
                    val fridayObject = Day("Friday", fridayStart, fridayEnd)
                    dayArray.add(fridayObject)
                    //saturday
                    val saturday = content.getJSONObject("saturday")
                    val saturdayStart = saturday.getString("start")
                    val saturdayEnd = saturday.getString("end")
                    val saturdayObject = Day("Saturday", saturdayStart, saturdayEnd)
                    dayArray.add(saturdayObject)
                    //sunday
                    val sunday = content.getJSONObject("sunday")
                    val sundayStart = sunday.getString("start")
                    val sundayEnd = sunday.getString("end")
                    val sundayObject = Day("Sunday", sundayStart, sundayEnd)
                    dayArray.add(sundayObject)
                    //make schedule object for the day
                    val scheduleObject = Schedule(name, start, end, dayArray)
                    semesterArray.add(scheduleObject)
                }
                val currentsched = semesterArray.last()
                //println("${currentsched.name}")
                var scheduleString = ""
                scheduleString += "semester:" +currentsched.name + ":\n"
                for(i in 0 until currentsched.content.size){
                    scheduleString+= currentsched.content[i].name + "\nHours: "+currentsched.content[i].start + " to "+currentsched.content[i].end +"\n\n"

                }
                infoTextView.text=scheduleString
            }
        })

        thread.start()
        //return semesterArray

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

}