package edu.rpi.shuttletracker

import android.os.Bundle
import android.util.Log
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class ScheduleActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // private string declare in the latter section of the program
        val jsonStr = listData
        try {

            // Create a userList string hashmap arraylist
            val userList = ArrayList<HashMap<String, String?>>()

            // Declaring the listView from the layout file
            val lv = findViewById<ListView>(R.id.schedulesList)

            // Initializing the JSON object and extracting the information
            val jObj = JSONObject(jsonStr)
            val jsonArry = jObj.getJSONArray("users")
            for (i in 0 until jsonArry.length()) {
                val user = HashMap<String, String?>()
                val obj = jsonArry.getJSONObject(i)
                user["name"] = obj.getString("name")
                user["designation"] = obj.getString("designation")
                user["location"] = obj.getString("location")
                userList.add(user)
            }

            // ListAdapter to broadcast the information to the list elements
            val adapter: ListAdapter = SimpleAdapter(
            this, userList, R.layout.schedule_list,
                arrayOf("name", "designation", "location"), intArrayOf(
                    R.id.name,
                    R.id.designation, R.id.location
                )
            )
            lv.adapter = adapter
            } catch (ex: JSONException) {
                Log.e("JsonParser Example", "unexpected JSON exception", ex)
            }
    }

    private val listData: String

        get() = ("{ \"schedules\" :[" +
                "{\"name\": \"Spring 2022\", \"start\": \"2022-01-23T07:00:00.000Z\", \"end\": \"2022-05-6T23:45:00.000Z\", \"content\": {" +
                    "{ \"day\": \"Monday\": \"start\": \"7:00\", \"end\": \"23:45\"}" +
                    "{ \"day\": \"Tuesday\": \"start\": \"07:00\", \"end\": \"23:45\"}" +
                    "{ \"day\": \"Wednesday\": \"start\": \"07:00\", \"end\": \"23:45\"}" +
                    "{ \"day\": \"Thursday\": \"start\": \"07:00\", \"end\": \"23:45\"}" +
                    "{ \"day\": \"Friday\": \"start\": \"07:00\", \"end\": \"23:45\"}" +
                    "{ \"day\": \"Saturday\": \"start\": \"09:00\", \"end\": \"23:45\"}" +
                    "{ \"day\": \"Sunday\": \"start\": \"09:00\", \"end\": \"8:00\"}}} ]}")
}