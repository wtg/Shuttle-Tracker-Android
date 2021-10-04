package edu.rpi.shuttletracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.net.URL
import org.json.JSONArray

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        val stopArray = ArrayList<Stop>()
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL("https://shuttletracker.app/stops")
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for(i in 0 until jsonArray.length()) {
                    val stop = jsonArray.getJSONObject(i)
                    val coordinate = stop.getJSONObject("coordinate")
                    val latitude = coordinate.getDouble("latitude")
                    val longitude = coordinate.getDouble("longitude")
                    val name = stop.getString("name")
                    val stopObject = Stop(latitude, longitude, name)
                    stopArray.add(stopObject)
                }
                for(i in 0 until stopArray.size) {
                    val current = stopArray.get(i)
                    val stopPos = LatLng(current.latitude, current.longitude)
                    runOnUiThread{mMap.addMarker(MarkerOptions().position(stopPos).title(current.name))}
                }
            }
        })
        thread.start()
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val Union = LatLng(42.730426, -73.676573)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Union))
    }
}

