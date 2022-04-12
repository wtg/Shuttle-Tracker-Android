package edu.rpi.shuttletracker;

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONArray
import java.net.URL


public class SettingsActivity: AppCompatActivity() {
//    fun drawBusIcons() {
//        val GPSbus : ImageView = findViewById(R.id.IMG_GPSbus)
//        var bMap = BitmapFactory.decodeFile(getString(R.string.GPS_bus))
//        GPSbus.setImageBitmap(bMap)
//
//        val crowdbus : ImageView = findViewById(R.id.IMG_crowdbus)
//        bMap = BitmapFactory.decodeFile(getString(R.string.crowdsourced_bus))
//        crowdbus.setImageBitmap(bMap)
//
//        val cbGPSbus : ImageView = findViewById(R.id.IMG_cbGPSbus)
//        bMap = BitmapFactory.decodeFile(getString(R.string.colorblind_GPS_bus))
//        cbGPSbus.setImageBitmap(bMap)
//
//        val cbcrowdbus : ImageView = findViewById(R.id.IMG_cbcrowdbus)
//        bMap = BitmapFactory.decodeFile(getString(R.string.colorblind_crowdsourced_bus))
//        cbcrowdbus.setImageBitmap(bMap)
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
//        drawBusIcons()
        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        if(sharedPreferences.contains("toggle_value")) {
            toggle.setChecked(loadToggle(this))
        }
        MapsActivity.colorblindMode.setMode(toggle.isChecked)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                MapsActivity.colorblindMode.setMode(true)
                saveToggle(this, true)
            } else {
                MapsActivity.colorblindMode.setMode(false)
                saveToggle(this, false)
            }
        }
        val toolbar: Toolbar = findViewById(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setDisplayShowHomeEnabled(true)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onResume() {
        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onResume()
        toggle.setChecked(loadToggle(this))
        MapsActivity.colorblindMode.setMode(toggle.isChecked)
    }
    override fun onPause() {
        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onPause()
        saveToggle(this, toggle.isChecked)
    }
    override fun onStart() {
        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onStart()
        toggle.setChecked(loadToggle(this))
        MapsActivity.colorblindMode.setMode(toggle.isChecked)

        println()
    }
    override fun onStop() {
        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onStop()
        saveToggle(this, toggle.isChecked)
    }
    private fun saveToggle(context: Context, isToggled: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("toggle_value", isToggled).apply()
    }

    private fun loadToggle(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("toggle_value", true)
    }

    private fun scheduleAPI(url: String) : ArrayList<Schedule>{
        val semesterArray = ArrayList<Schedule>()
        val thread = Thread(kotlinx.coroutines.Runnable {
            kotlin.run {
                val url = URL(url)
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val semSchedule = jsonArray.getJSONObject(i)
                    val name = semSchedule.getString("name")
                    val start = semSchedule.getString("start")
                    val end = semSchedule.getString("end")
                    val content = semSchedule.getJSONArray("content")
                    val dayArray = ArrayList<Day>()

                    //monday
                    val monday = content.getJSONObject(0)
                    val dayStart = monday.getString("start")
                    val dayEnd = monday.getString("end")
                    val dayObject = Day("Monday", dayStart, dayEnd)
                    dayArray.add(dayObject)
                    //tuesday
                    val tuesday = content.getJSONObject(1)
                    val tuesdayStart = tuesday.getString("start")
                    val tuesdayEnd = tuesday.getString("end")
                    val tuesdayObject = Day("Tuesday", tuesdayStart, tuesdayEnd)
                    dayArray.add(tuesdayObject)
                    //wednesday
                    val wednesday = content.getJSONObject(2)
                    val wednesdayStart = wednesday.getString("start")
                    val wednesdayEnd = wednesday.getString("end")
                    val wednesdayObject = Day("Wednesday", wednesdayStart, wednesdayEnd)
                    dayArray.add(wednesdayObject)
                    //thursday
                    val thursday = content.getJSONObject(3)
                    val thursdayStart = thursday.getString("start")
                    val thursdayEnd = thursday.getString("end")
                    val thursdayObject = Day("Thursday", thursdayStart, thursdayEnd)
                    dayArray.add(thursdayObject)
                    //friday
                    val friday = content.getJSONObject(4)
                    val fridayStart = friday.getString("start")
                    val fridayEnd = friday.getString("end")
                    val fridayObject = Day("Friday", fridayStart, fridayEnd)
                    dayArray.add(fridayObject)
                    //saturday
                    val saturday = content.getJSONObject(5)
                    val saturdayStart = saturday.getString("start")
                    val saturdayEnd = saturday.getString("end")
                    val saturdayObject = Day("Saturday", saturdayStart, saturdayEnd)
                    dayArray.add(saturdayObject)
                    //sunday
                    val sunday = content.getJSONObject(6)
                    val sundayStart = sunday.getString("start")
                    val sundayEnd = sunday.getString("end")
                    val sundayObject = Day("Sunday", sundayStart, sundayEnd)
                    dayArray.add(sundayObject)
                    //make schedule object for the day
                    val scheduleObject = Schedule(name, start, end, dayArray)
                    semesterArray.add(scheduleObject)
                }
                val currentsched = semesterArray.last()
                for(i in 0 until currentsched.content.size){

                }
            }
        })
        thread.start()
        return semesterArray
    }
}
