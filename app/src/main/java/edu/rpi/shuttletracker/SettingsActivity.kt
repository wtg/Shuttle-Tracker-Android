package edu.rpi.shuttletracker;

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
//        drawBusIcons()

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)

        val serverURLText = findViewById<EditText>(R.id.editServerURL)
        val saveServerURLButton = findViewById<Button>(R.id.saveURLButton)

        saveServerURLButton.setOnClickListener {
            saveServerURL(this)
        }

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

    private fun saveServerURL(context: Context) {
        Log.d("server_save_button", "saveServerURL called")
        val serverURLText = findViewById<EditText>(R.id.editServerURL)
        val serverURL = serverURLText.text.toString()

//        Log.d("server_save_button", "msg 1 " + serverURL)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("server_base_url", serverURL).apply()
//        val string1 = sharedPreferences.getString("server_base_url", "NOT SPECIFIED")
//        if (string1 != null) {
//            Log.d("server_save_button", string1)
//        }

    }

}
