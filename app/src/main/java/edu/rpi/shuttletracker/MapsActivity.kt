package edu.rpi.shuttletracker

import android.Manifest
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
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.Global.getString
import android.provider.Settings.System.getString
import android.system.Os.accept
import android.view.animation.AnimationUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.rotationMatrix
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.*
/*
import kotlinx.android.synthetic.main.activity_maps.fabBGLayout
import kotlinx.android.synthetic.main.activity_maps.fab
import kotlinx.android.synthetic.main.activity_maps.fabLayout1
import kotlinx.android.synthetic.main.activity_maps.fabLayout2
import kotlinx.android.synthetic.main.activity_maps.fabLayout3
import kotlinx.android.synthetic.main.activity_maps.fabLayout4
*/
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI.create
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.system.*
import kotlin.coroutines.*


import com.google.android.gms.maps.MapView

import android.view.ViewGroup

import android.view.LayoutInflater




class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var busMarkerArray: ArrayList<Marker> = ArrayList<Marker>()
    private var busesDrawn : Boolean = false

    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private var stopArray = ArrayList<Stop>()
    private val busArray = ArrayList<Bus>()


    // All data used in a data package which will be sent to server
    private var onBus: Boolean = false // This user's status. It controls the end of data transmission thread.
    private var selectedBusNumber: String? = null
    private lateinit var session_uuid: String
    private var currentLocation: Location? = null
    //private var latitude: Float? = null
    //private var longitude: Float? = null
    private var type = "user"
    private lateinit var date: String

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
        fab.setOnClickListener {
            if (View.GONE == fabBGLayout.visibility) {
                showFABMenu()
            } else {
                closeFABMenu()
            }
        }

        fabBGLayout.setOnClickListener { closeFABMenu() }
        var btn_settings = findViewById<LinearLayout>(R.id.fabLayout1)
        var btn_about = findViewById<LinearLayout>(R.id.fabLayout2)
        var btn_info = findViewById<LinearLayout>(R.id.fabLayout3)
        val boardBusButton = findViewById<Button>(R.id.board_bus_button)
        val leaveBusButton = findViewById<Button>(R.id.leave_bus_button)

        //placement
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // Initialize location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5*1000 // refreshes every 5 seconds
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())






        btn_settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        btn_info.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }
        btn_about.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        boardBusButton.setOnClickListener {
            /**
             *  1. get available bus numbers from server
             *  2. start a alert dialog to let user choose which bus to board
             *  3. send data to server
             *  4. update this client's state and change the button to "leave bus"
             */
            println("location: $currentLocation") // TODO: remove/comment this testing clause

            // Check if the user is near a bus stop. If not, pop up an alert dialog and stop this button's onclick listener.
            if (!checkNearbyStop()) {
                return@setOnClickListener
            }

            val busNumberArray = getAvailableBusNumbers().sorted().map { it.toString() }
                .toTypedArray() // convert Array<Int> to Array<String>

            // Given an array of bus numbers, create an AlertDialog to let the user choose which bus to board.
            val chooseBusDialogBuilder = AlertDialog.Builder(this)
            chooseBusDialogBuilder.setTitle("Bus Selection")

                // TODO: Find closest bus and recommend this bus to the user.
                
                .setSingleChoiceItems(busNumberArray, -1) { _, which ->
                    selectedBusNumber = busNumberArray[which]
                }
                .setPositiveButton("Continue") { dialog, _ ->
                    if (selectedBusNumber != null) {
                        val sendDataThread = sendOnBusData()
                        sendDataThread.start()

                        // hide the dialog
                        dialog.cancel()

                        // switch buttons by changing their visibility
                        boardBusButton.visibility = View.GONE
                        leaveBusButton.visibility = View.VISIBLE
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
            chooseBusDialogBuilder.create()
            chooseBusDialogBuilder.show()
        };
        leaveBusButton.setOnClickListener {
            onBus = false // this variable controls when the data-transmitting thread ends
            boardBusButton.visibility = View.VISIBLE
            leaveBusButton.visibility = View.GONE
        };
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    }

    /**
     *  Run the is-near-any-stop check. If user is not near any stop, pop up an AlertDialog.
     *  @return true if the user is within 20 meters of any stop; otherwise false.
     */
    private fun checkNearbyStop(): Boolean {
        return if (!isNearStop()) {
            val noNearbyStopDialogBuilder = AlertDialog.Builder(this)
            val noNearbyStopMessage = "You can't board a bus if you're not within 20 meters of a stop."
            noNearbyStopDialogBuilder.setTitle("No Nearby Stop")
                .setMessage(noNearbyStopMessage)
                .setNegativeButton("Continue") { dialog, _ ->
                    println("Location Check: Not near a stop") // TODO: remove/comment this testing clause
                    dialog.cancel()
                }
            noNearbyStopDialogBuilder.create()
            noNearbyStopDialogBuilder.show()
            false
        } else {
            true
        }
    }

    /**
     *  Checks if this user is within 20 meters of any bus stop.
     */
    private fun isNearStop(): Boolean {
        //updateCurrentLocation()
        for (stop in stopArray) {
            val stopLocation = Location("stop location")
            stopLocation.latitude = stop.latitude
            stopLocation.longitude = stop.longitude
            //println("current location: $currentLocation") // // TODO: remove/comment this testing clause
            //println("stop location: $stopLocation") // // TODO: remove/comment this testing clause
            if (currentLocation?.distanceTo(stopLocation)!! <= 20) {
                return true
            }
        }
        return false
    }

    /**
     *  Keep sending data every 5 seconds.
     *  @return A thread which keeps sending data packs to the server every 5 seconds.
     */
    private fun sendOnBusData(): Thread {
        session_uuid = getRandomSessionUuid()
        println("session_uuid: $session_uuid") // TODO: remove/comment this testing clause

        val thread = Thread {
            kotlin.run {
                onBus = true
                while (onBus) {
                    date = getCurrentFormattedDate()
                    println("parsed date: $date") // TODO: remove/comment this testing clause
                    //updateCurrentLocation()

                    // TODO: remove this testing location when committing
                    //latitude = (42.722886).toFloat()
                    //longitude = (-73.679665).toFloat()

                    val boardBusJSONObject = parseDataToJSONObject(
                        session_uuid,
                        currentLocation?.latitude?.toFloat(),
                        currentLocation?.longitude?.toFloat(),
                        type,
                        getCurrentFormattedDate()
                    )
                    println("parsed JSONObject: $boardBusJSONObject") // TODO: remove/comment this testing clause
                    val boardBusUrl =
                        URL(resources.getString(R.string.buses_url) + "/$selectedBusNumber")
                    println("Target URL: $boardBusUrl") // TODO: remove/comment this testing clause

                    // send to server
                    val request = Request.Builder()
                        .url(boardBusUrl)
                        .patch(
                            boardBusJSONObject.toString().toRequestBody(mediaType)
                        )
                        .build()
                    println("Request: $request") // TODO: remove/comment this testing clause

                    val response = httpClient.newCall(request).execute()
                    println("response: $response") // TODO: remove/comment this testing clause

                    // wait for 5 seconds
                    Thread.sleep(5000L)
                }
            }
        }
        return thread
    }

    /**
     * Start a thread to get all available bus numbers from server.
     * A request is sent to [https://shuttletracker.app/buses/all].
     *
     * @return An integer array of bus numbers.
     */
    private fun getAvailableBusNumbers(): ArrayList<Int> {
        val busNumberArray = ArrayList<Int>()

        // start the thread and wait for it to finish
        val thread = Thread {
            kotlin.run {
                val url = URL(resources.getString(R.string.bus_numbers_url))
                val jsonString = url.readText()
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    busNumberArray.add(jsonArray.getInt(i))
                }
            }
        }
        thread.start()
        thread.join()

        return busNumberArray
    }

    /**
     *  Generate a random session uuid served as this user's identifier.
     *  @return A randomly generated string to be used as session uuid.
     */
    private fun getRandomSessionUuid(): String {
        return UUID.randomUUID().toString()
    }

    /**
     *  Get the current date time in the format of ISO-8601 (e.g. 2021-11-12T22:44:55+00:00), excluding milliseconds.
     *  @return An ISO-8601 date string.
     */
    private fun getCurrentFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC") // use UTC as default time zone

        return sdf.format(Date())
    }

    /**
     *  Parse all data of a board-bus activity into a JSONObject.
     *  @return A JSONObject including session_uuid, coordinate, type and date.
     */
    private fun parseDataToJSONObject(session_uuid: String, latitude: Float?, longitude: Float?, type: String, date: String): JSONObject {
        val coordinate = mapOf("latitude" to latitude, "longitude" to longitude)
        var jsonMap = mapOf("id" to session_uuid, "coordinate" to coordinate, "type" to type, "date" to date)
        return JSONObject(jsonMap)
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

    //Updates buses to the proper colorblind setting when the Map gets opened
    override fun onResume() {
        super.onResume()
        val res : Resources = getResources()
        if(!busesDrawn) {
            busMarkerArray = drawBuses(res.getString(R.string.buses_url))
        } else {
            busMarkerArray = updateBuses(res.getString(R.string.buses_url), busMarkerArray)
        }
    }


    fun drawStops(url: String) : ArrayList<Stop> {
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
        return stopArray;
    }

    fun scheduleAPI(url: String) : ArrayList<Schedule>{
        val semesterArray = ArrayList<Schedule>()
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL(url)
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val semSchedule = jsonArray.getJSONObject(i)
                    val name = semSchedule.getString("name")
                    val start = semSchedule.getString("start")
                    val end = semSchedule.getString("end")
                    val content = semSchedule.getJSONArray("content")
                    val dayArray =  ArrayList<Day>()

                    for(j in 0 until content.length()){
                        val dayName = content.getJSONObject(j)
                        val dayStart = dayName.getString("start")
                        val dayEnd = dayName.getString("end")
                    }
                    //val stopObject = Stop(latitude, longitude, name)
                    //stopArray.add(stopObject)
                }
            }
        }
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
                            .color(Color.RED)
                            .width(4F)
                    )
                }
            }
        })
        thread2.start()
    }

    //@RequiresApi(Build.VERSION_CODES.O)
    fun drawBuses(url: String): ArrayList<Marker> {
        //val busArray = ArrayList<Bus>()
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
        busesDrawn = true
        return markerArray
    }
    //@RequiresApi(Build.VERSION_CODES.O)
    fun updateBuses(url: String, markerArray: ArrayList<Marker>): ArrayList<Marker> {
        if(markerArray.size == 0 && !busesDrawn) {
            return markerArray
        }
        println("updateBuses() called")
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
                            if (markerArray.get(i).tag!!.equals(id)) {
                                found = true
                                markerArray.get(i).setPosition(LatLng(latitude, longitude))
                                markerArray.get(i).setIcon(BitmapDescriptorFactory.fromAsset(busIcon))
                                println("Bus " + id + " updated.")
                            }
                        }
                    }
                    runOnUiThread {
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
        stopArray = drawStops(res.getString(R.string.stops_url))
        drawRoutes(res.getString(R.string.routes_url))
        val busTimer = Timer("busTimer", true)

//        if(APIMatch)
        if(!busesDrawn) {
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
                busMarkerArray = updateBuses(res.getString(R.string.buses_url), busMarkerArray)
            //println("Updated bus locations.")
        }

        var btn_refresh = findViewById(R.id.fab4) as FloatingActionButton
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        btn_refresh.animation = rotate
        btn_refresh.setOnClickListener {

            btn_refresh.startAnimation(rotate)
            Toast.makeText(applicationContext, "Refreshed!", Toast.LENGTH_SHORT).show()

            busMarkerArray = updateBuses(res.getString(R.string.buses_url), busMarkerArray)
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
                    fusedLocationClient.lastLocation.addOnSuccessListener { location:Location ->
                        currentLocation = location // update current location
                        println("current location updated") // TODO: remove/comment this testing clause
                    }

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

