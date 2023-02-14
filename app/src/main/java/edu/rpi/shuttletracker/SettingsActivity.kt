package edu.rpi.shuttletracker;

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.activity_maps.*



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

//    private var EditText url_settings_view = (EditText) findViewById(r.id.editServerURL);



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
//        drawBusIcons()

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val res: Resources = getResources()

        val colorBlindToggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        val logsToggle: SwitchMaterial = findViewById(R.id.logsSwitch)

        val serverURLText = findViewById<EditText>(R.id.editServerURL)
        serverURLText.setText(sharedPreferences.getString("server_base_url", res.getString(R.string.default_server_url)))
        val saveServerURLButton = findViewById<Button>(R.id.saveURLButton)

        val resetURLButton = findViewById<Button>(R.id.resetURLButton)

        saveServerURLButton.setOnClickListener {
            saveServerURL(this)
        }

        resetURLButton.setOnClickListener{
            resetServerURL(this)
        }

        Log.d("log_save", "shared pref color blind is: " + sharedPreferences.getBoolean("colorblind_toggle_value", false))

        if(sharedPreferences.contains("colorblind_toggle_value")) {
            colorBlindToggle.setChecked(loadColorBlindToggle(this))
        }
        MapsActivity.colorblindMode.setMode(colorBlindToggle.isChecked)
        colorBlindToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                MapsActivity.colorblindMode.setMode(true)
                saveColorBlindToggle(this, true)
            } else {
                MapsActivity.colorblindMode.setMode(false)
                saveColorBlindToggle(this, false)
            }
        }

        logsToggle.setChecked(sharedPreferences.getBoolean("logs_toggle_value", true))
        logsToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                saveLogsToggle(this, true)
            } else {
                saveLogsToggle(this, false)
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
        val colorBlindToggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onResume()
        colorBlindToggle.setChecked(loadColorBlindToggle(this))
        MapsActivity.colorblindMode.setMode(colorBlindToggle.isChecked)
    }
    override fun onPause() {
        val colorBlindToggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onPause()
        saveColorBlindToggle(this, colorBlindToggle.isChecked)
    }
    override fun onStart() {
        val colorBlindToggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onStart()
        colorBlindToggle.setChecked(loadColorBlindToggle(this))
        MapsActivity.colorblindMode.setMode(colorBlindToggle.isChecked)
    }
    override fun onStop() {
        val colorBlindToggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        super.onStop()
        saveColorBlindToggle(this, colorBlindToggle.isChecked)
    }
    private fun saveColorBlindToggle(context: Context, isToggled: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("colorblind_toggle_value", isToggled).apply()
    }

    private fun loadColorBlindToggle(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("colorblind_toggle_value", false)
    }

    private fun saveLogsToggle(context: Context, isToggled: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("logs_toggle_value", isToggled).apply()
        Log.d("log_save", "logs toggle value is: " + sharedPreferences.getBoolean("logs_toggle_value", true))
    }

    private fun saveServerURL(context: Context) {
        val serverURLText = findViewById<EditText>(R.id.editServerURL)
        val serverURL = serverURLText.text.toString()

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("server_base_url", serverURL).apply()

        val text = "Saved URL"
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.show()
    }

    private fun resetServerURL(context: Context){
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val res: Resources = getResources()
        val editor = sharedPreferences.edit()
        editor.putString("server_base_url", res.getString(R.string.default_server_url)).apply()
        val serverURLText = findViewById<EditText>(R.id.editServerURL)
        serverURLText.setText(sharedPreferences.getString("server_base_url", res.getString(R.string.default_server_url)))

        val text = "URL Reset"
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.show()
    }

}
