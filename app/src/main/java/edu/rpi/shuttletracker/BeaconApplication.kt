package edu.rpi.shuttletracker

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import org.altbeacon.beacon.*

class BeaconApplication : Application() {
    private val TAG = "Beacon"
    private val UUID = 1

    override fun onCreate() {
        super.onCreate()

        /*val beaconManager =  BeaconManager.getInstanceForApplication(this)
        val region = Region("all-beacons-region", null, null, null)
        beaconManager.beaconParsers.clear();
        beaconManager.beaconParsers.add(
            BeaconParser().
            setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        beaconManager.getRegionViewModel(region).regionState.observeForever(monitoringObserver)
        beaconManager.startMonitoring(region)*/
    }

    val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            println("detected beacon")
            Log.d(TAG, "Detected beacons(s)")
        }
        else {
            println("no longer detecting beacons")
            Log.d(TAG, "Stopped detecting beacons")
        }
    }

    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        for (beacon: Beacon in beacons) {
            Log.d(TAG, "$beacon id1 ${beacon.id1} ")
        }
    }
}