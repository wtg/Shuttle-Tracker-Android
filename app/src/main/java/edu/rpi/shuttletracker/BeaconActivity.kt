package edu.rpi.shuttletracker

import android.Manifest
import android.R
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.altbeacon.beacon.MonitorNotifier

public class BeaconActivity : AppCompatActivity() {
    private val TAG = "Beacon"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            Log.d(TAG, "Detected beacons(s)")
        }
        else {
            Log.d(TAG, "Stopped detecting beacons")
        }
    }
}