package edu.rpi.shuttletracker;

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.activity_maps.*
import android.content.SharedPreferences
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar


public class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
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

}
