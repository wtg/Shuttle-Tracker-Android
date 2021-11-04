package edu.rpi.shuttletracker

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.Transformations.map
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.*
import org.json.JSONArray
import java.net.URL
import java.security.AccessController.getContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate
import android.content.Intent
import android.net.Uri
import android.system.Os.accept
import com.google.android.material.dialog.MaterialAlertDialogBuilder


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


    fun drawStops(url: String) {
        val stopArray = ArrayList<Stop>()
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL(url)
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val stop = jsonArray.getJSONObject(i)
                    val coordinate = stop.getJSONObject("coordinate")
                    val latitude = coordinate.getDouble("latitude")
                    val longitude = coordinate.getDouble("longitude")
                    val name = stop.getString("name")
                    val stopObject = Stop(latitude, longitude, name)
                    stopArray.add(stopObject)
                }
                for (i in 0 until stopArray.size) {
                    val current = stopArray.get(i)
                    val stopPos = LatLng(current.latitude, current.longitude)
                    runOnUiThread {
                        mMap.addMarker(
                            MarkerOptions().position(stopPos).title(current.name).icon(
                                BitmapDescriptorFactory.fromAsset("simplecircle.png")
                            )
                        )
                    }
                }
            }
        })
        thread.start()
    }
    fun drawRoutes(url: String) {
        val thread2 = Thread(Runnable {
            kotlin.run {
                val url = URL(url)
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                var routeObject = jsonArray.getJSONObject(0)
                var coordArray = routeObject.getJSONArray("coordinates")
                var latlngarr = ArrayList<LatLng>()
                for (i in 0 until coordArray.length()) {
                    val waypoint = coordArray.getJSONObject(i)
                    val latitude = waypoint.getDouble("latitude")
                    val longitude = waypoint.getDouble("longitude")
                    val latlng = LatLng(latitude, longitude)
                    latlngarr.add(latlng)
                }
                runOnUiThread {
                    val polyline1 = mMap.addPolyline(
                        PolylineOptions()
                            .clickable(true)
                            .addAll(latlngarr)
                    )
                }
            }
        })
        thread2.start()
    }

    //@RequiresApi(Build.VERSION_CODES.O)
    fun drawBuses(url: String): ArrayList<Marker> {
        val busArray = ArrayList<Bus>()
        var markerArray = ArrayList<Marker>()
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL(url)
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val bus = jsonArray.getJSONObject(i)
                    val location = bus.getJSONObject("location")
                    val date = location.getString("date")
                    val coordinate = location.getJSONObject("coordinate")
                    val latitude = coordinate.getDouble("latitude")
                    val longitude = coordinate.getDouble("longitude")
                    val id = bus.getInt("id")
                    val busType = location.getString("type")
                    var busIcon = "redbus.png"
                    if(busType == "user") {
                        busIcon = "greenbus.png"
                    }
                    val busObject = Bus(latitude, longitude, id, busIcon)
                    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    val busDate: LocalDateTime = LocalDateTime.parse(
                        date,
                        format//DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    )
                    val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
//                    println(busDate)
//                    println(currentDate)
                    val minutes: Long = ChronoUnit.MINUTES.between(busDate, currentDate)
                    val hours: Long = ChronoUnit.HOURS.between(busDate, currentDate)
                    val days: Long = ChronoUnit.DAYS.between(busDate, currentDate)
                    if (days == 0.toLong() && hours == 0.toLong() && minutes < 5) {
                        busArray.add(busObject)
                    }
                }
                for (i in 0 until busArray.size) {
                    val current = busArray.get(i)
                    val stopPos = LatLng(current.latitude, current.longitude)
                    runOnUiThread {
                        markerArray.add(
                            mMap.addMarker(
                                MarkerOptions().position(stopPos).title(
                                    "Bus " + current.id
                                ).icon(
                                    BitmapDescriptorFactory.fromAsset(current.busIcon)
                                )
                            )
                        )
                        markerArray.get(i).tag = current.id;
                    }
                }
            }
        })
        thread.start()
        return markerArray
    }
    //@RequiresApi(Build.VERSION_CODES.O)
    fun updateBuses(url: String, markerArray: ArrayList<Marker>): ArrayList<Marker> {
        val busArray = ArrayList<Bus>()
        //var markerArray = ArrayList<Marker>()
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL(url)
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val bus = jsonArray.getJSONObject(i)
                    val id = bus.getInt("id")
                    val location = bus.getJSONObject("location")
                    val date = location.getString("date")
                    val coordinate = location.getJSONObject("coordinate")
                    val latitude = coordinate.getDouble("latitude")
                    val longitude = coordinate.getDouble("longitude")
                    val busType = location.getString("type")
                    var busIcon = "redbus.png"
                    if(busType == "user") {
                        busIcon = "greenbus.png"
                    }
                    val busObject = Bus(latitude, longitude, id, busIcon)
                    var found = false
                    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    val busDate = LocalDateTime.parse(date, format)
                    for (i in 0 until markerArray.size) {
                        runOnUiThread {
                            if (markerArray.get(i).tag == id) {
                                found = true
                                markerArray.get(i).setPosition(LatLng(latitude, longitude))
                                markerArray.get(i).setIcon(BitmapDescriptorFactory.fromAsset(busIcon))
                                println("Bus " + id + " updated.")
                            }
                            if (!found) {
                                val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC);
                                val minutes: Long = ChronoUnit.MINUTES.between(busDate, currentDate)
                                val hours: Long = ChronoUnit.HOURS.between(busDate, currentDate)
                                val days: Long = ChronoUnit.DAYS.between(busDate, currentDate)
                                if (days == 0.toLong() && hours == 0.toLong() && minutes < 5) {
                                    busArray.add(busObject)
                                }
                            }
                        }
                    }
                    for (i in 0 until busArray.size) {
                        val current = busArray.get(i)
                        val stopPos = LatLng(current.latitude, current.longitude)
                        runOnUiThread {
                            markerArray.add(
                                mMap.addMarker(
                                    MarkerOptions().position(stopPos).title(
                                        "Bus " + current.id
                                    ).icon(
                                        BitmapDescriptorFactory.fromAsset(current.busIcon)
                                    )
                                )
                            )
                            markerArray.get(i).tag = current.id;
                        }
                    }
                }
            }
        })
        thread.start()
        return markerArray
    }

    fun APIVersionMatch(currentAPI: Int, website: String): Boolean {
        var number = 0
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL(website)
                val data = url.readText()
                number = data.toInt()
            }
        })
        thread.start()
        return currentAPI == number
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
    //@RequiresApi(Build.VERSION_CODES.O)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val currentAPI = 0
        val APIMatch = APIVersionMatch(currentAPI, "https://shuttletracker.app/version")
        if(APIMatch) {
            drawStops("https://shuttletracker.app/stops")
            drawRoutes("https://shuttletracker.app/routes")
        } else {
            val contextView = findViewById<View>(R.id.map)
//            Snackbar.make(contextView, "Your app is outdated and no longer works.", Snackbar.LENGTH_LONG)
//                .setAction("Update") {
//                    val browserIntent = Intent(
//                        Intent.ACTION_VIEW,
//                        Uri.parse("http://www.google.com")
//                    )
//                    startActivity(browserIntent)
//                }
//                .show()
            MaterialAlertDialogBuilder(this)
                .setMessage("Your app is outdated and no longer works. Please update it to restore shuttle tracking functionality.")
//                .setNegativeButton("Later") { dialog, which ->
//                    // Respond to negative button press
//                }
                .setPositiveButton("Update") { dialog, which ->
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.duckduckgo.mobile.android&hl=en_US&gl=US")
                    )
                    startActivity(browserIntent)
                }
                .show()
        }
//        fixedRateTimer("timer", false, 0L, 60 * 1000) {
//            runOnUiThread {
//                tvTime.text = SimpleDateFormat("dd MMM - HH:mm", Locale.US).format(Date())
//            }
//        }
        // Add a marker in Sydney and move the camera
        val Union = LatLng(42.730426, -73.676573)
        mMap.setMinZoomPreference(13.5f)
        mMap.setMaxZoomPreference(20.0f)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Union))
//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        actionBar?.hide()
        val currentNightMode =  resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {} // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {googleMap.setMapStyle(MapStyleOptions(getResources()
                .getString(R.string.style_json)));} // Night mode is active, we're using dark theme
        }
        val busTimer = Timer("busTimer", true)
        var busMarkerArray: ArrayList<Marker> = ArrayList<Marker>()
        if(APIMatch)
            busMarkerArray = drawBuses("https://shuttletracker.app/buses")
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
        busTimer.scheduleAtFixedRate(0, 5000) {
            if(APIMatch)
                busMarkerArray = updateBuses("https://shuttletracker.app/buses", busMarkerArray)
            //println("Updated bus locations.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    mMap.setMyLocationEnabled(true)
                }
            }
        }
    }
    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }


}

