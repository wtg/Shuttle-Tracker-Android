package edu.rpi.shuttletracker

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.animation.Animator
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.Runnable
import org.json.JSONArray
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate




class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    object colorblindMode : Application() {
        var colorblind : Boolean = false
        fun getMode() : Boolean {
            return colorblind
        }
        fun setMode(mode : Boolean) {
            colorblind = mode
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_maps)

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
            fab.setOnClickListener {
                if (View.GONE == fabBGLayout.visibility) {
                    showFABMenu()
                } else {
                    closeFABMenu()
                }
            }

            fabBGLayout.setOnClickListener { closeFABMenu() }
            var btn_settings = findViewById(R.id.fabLayout1) as LinearLayout
            var btn_about = findViewById(R.id.fabLayout2) as LinearLayout
            var btn_info = findViewById(R.id.fabLayout3) as LinearLayout


            btn_settings.setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent);
            }
            btn_info.setOnClickListener {
                val intent = Intent(this, InfoActivity::class.java)
                startActivity(intent);
            }
            btn_about.setOnClickListener {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent);
            }

    }

    private fun showFABMenu() {
        fabLayout1.visibility = View.VISIBLE
        fabLayout2.visibility = View.VISIBLE
        fabLayout3.visibility = View.VISIBLE
        //fablayout4 (the refresh button) is already visible at the start
        fabBGLayout.visibility = View.VISIBLE
        fab.animate().rotationBy(180F)
        fabLayout1.animate().translationY(-resources.getDimension(R.dimen.standard_75))
        fabLayout2.animate().translationY(-resources.getDimension(R.dimen.standard_135))
        fabLayout3.animate().translationY(-resources.getDimension(R.dimen.standard_215))
        fabLayout4.animate().translationY(-resources.getDimension(R.dimen.standard_210))
        var btn_info = findViewById(R.id.fabLayout3) as LinearLayout
        btn_info.bringToFront()
    }

    private fun closeFABMenu() {
        fabBGLayout.visibility = View.GONE
        fab.bringToFront()
        fab.animate().rotation(0F)
        fabLayout1.animate().translationY(0f)
        fabLayout2.animate().translationY(0f)
        fabLayout3.animate().translationY(0f)
        fabLayout4.animate().translationY(0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {}
                override fun onAnimationEnd(animator: Animator) {
                    if (View.GONE == fabBGLayout.visibility) {
                        fabLayout1.visibility = View.GONE
                        fabLayout2.visibility = View.GONE
                        fabLayout3.visibility = View.GONE
                    }
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            //R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun internet_connection(): Boolean {
        //Check if connected to internet, output accordingly
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val networkCapabilities = cm.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun drawStops(url: String) {

        val stopArray = ArrayList<Stop>()
        if(internet_connection()){
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
        }else{
            println("connection be not working--drawstops")
        }
    }

    fun drawRoutes(url: String) {
        val thread2 = Thread(Runnable {
            if(internet_connection()){
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
                            .color(Color.RED)
                            .width(4F)
                    )
                }}
            }else{
                println("connection be not working--drawroute")
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
                    var busIcon = getString(R.string.GPS_bus)
                    if(colorblindMode.getMode()) {
                        if(busType == "user") {
                            busIcon = getString(R.string.colorblind_crowdsourced_bus)
                        } else {
                            busIcon = getString(R.string.colorblind_GPS_bus)
                        }
                    } else {
                        if(busType == "user") {
                            busIcon = getString(R.string.crowdsourced_bus)
                        }
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
                                ).zIndex(1F)
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
                    var busIcon = getString(R.string.GPS_bus)
                    if(colorblindMode.getMode()) {
                        if(busType == "user") {
                            busIcon = getString(R.string.colorblind_crowdsourced_bus)
                        } else {
                            busIcon = getString(R.string.colorblind_GPS_bus)
                        }
                    } else {
                        if(busType == "user") {
                            busIcon = getString(R.string.crowdsourced_bus)
                        }
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
                                    ).zIndex(1F)
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
//    fun APIPull(result: Data<Int>, website: String): Int {
//        val thread = Thread(Runnable {
//            kotlin.run {
//                val url = URL(website)
//                val data = url.readText()
//                result.value = data.toInt()
//            }
//        })
//        thread.start()
//        return result.value
//    }
//    fun APIVersionMatch(currentAPI: Int, website: String): Boolean {
//        val data = Data<Int>(0)
//        var number : Int
//        number = async { APIPull(data, website) }
//        return data.value == currentAPI
//    }

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
        mMap.getUiSettings().setMapToolbarEnabled(false)
        val currentNightMode =  resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {} // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {googleMap.setMapStyle(MapStyleOptions(getResources()
                .getString(R.string.style_json)));} // Night mode is active, we're using dark theme
        }
        val Union = LatLng(42.730426, -73.676573)
        mMap.setMinZoomPreference(13.5f)
        mMap.setMaxZoomPreference(20.0f)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Union))
        val res : Resources = getResources()
        //val currentAPI = 1
        //val APIMatch = APIVersionMatch(currentAPI, res.getString(R.string.version_url))
//        if(APIMatch) {

//        } else {
//            val contextView = findViewById<View>(R.id.map)
////            Snackbar.make(contextView, "Your app is outdated and no longer works.", Snackbar.LENGTH_LONG)
////                .setAction("Update") {
////                    val browserIntent = Intent(
////                        Intent.ACTION_VIEW,
////                        Uri.parse("http://www.google.com")
////                    )
////                    startActivity(browserIntent)
////                }
////                .show()
//            MaterialAlertDialogBuilder(this)
//                .setMessage("Your app is outdated and no longer works. Please update it to restore shuttle tracking functionality.")
////                .setNegativeButton("Later") { dialog, which ->
////                    // Respond to negative button press
////                }
//                .setPositiveButton("Update") { dialog, which ->
//                    val browserIntent = Intent(
//                        Intent.ACTION_VIEW,
//                        Uri.parse("https://play.google.com/store/apps/details?id=edu.rpi.shuttletracker")
//                    )
//                    startActivity(browserIntent)
//                }
//                .show()
//        }
//        fixedRateTimer("timer", false, 0L, 60 * 1000) {
//            runOnUiThread {
//                tvTime.text = SimpleDateFormat("dd MMM - HH:mm", Locale.US).format(Date())
//            }
//        }
        // Add a marker in Sydney and move the camera
//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        actionBar?.hide()
        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        if(sharedPreferences.contains("toggle_value")) {
            colorblindMode.setMode(sharedPreferences.getBoolean("toggle_value", true))
        }
            drawStops(res.getString(R.string.stops_url))
            drawRoutes(res.getString(R.string.routes_url))
        val busTimer = Timer("busTimer", true)
        var busMarkerArray: ArrayList<Marker> = ArrayList<Marker>()
//        if(APIMatch)
        if(internet_connection()){
            busMarkerArray = drawBuses(res.getString(R.string.buses_url))
        }
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
            //if(APIMatch)
            if(internet_connection()) {
                busMarkerArray = updateBuses(res.getString(R.string.buses_url), busMarkerArray)
            }
            //println("Updated bus locations.")
        }







        //btn_refresh
        var btn_refresh = findViewById(R.id.fabLayout4) as LinearLayout
        btn_refresh.setOnClickListener {
            if(internet_connection()) {
                println("busMakerArrayentered")
                busMarkerArray = updateBuses(res.getString(R.string.buses_url), busMarkerArray)
                finish();
                startActivity(intent)

            }else{
                AlertDialog.Builder(this).setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again")
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(android.R.drawable.ic_dialog_alert).show()
            }
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

