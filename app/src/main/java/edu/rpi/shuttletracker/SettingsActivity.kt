package edu.rpi.shuttletracker;

import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.activity_maps.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.switchmaterial.SwitchMaterial

public class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        val toggle: SwitchMaterial = findViewById(R.id.colorblindSwitch)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {

            } else {
                // The toggle is disabled
            }
        }

    }
    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }

}
