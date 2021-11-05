package edu.rpi.shuttletracker;

import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.activity_maps.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

public class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
    }
    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }
}
