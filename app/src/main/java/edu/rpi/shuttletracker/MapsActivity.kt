package edu.rpi.shuttletracker

/*
import kotlinx.android.synthetic.main.activity_maps.fabBGLayout
import kotlinx.android.synthetic.main.activity_maps.fab
import kotlinx.android.synthetic.main.activity_maps.fabLayout1
import kotlinx.android.synthetic.main.activity_maps.fabLayout2
import kotlinx.android.synthetic.main.activity_maps.fabLayout3
import kotlinx.android.synthetic.main.activity_maps.fabLayout4
*/


import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.animation.Animator
import android.app.AlertDialog
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.Runnable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var busMarkerArray: ArrayList<Marker> = ArrayList<Marker>()
    private var busesDrawn : Boolean = false
    private var routeDrawn : Boolean = false
    private var alertPrompted : Boolean = false

    private lateinit var mMap: GoogleMap
    var APImatch : Boolean = false

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

    private lateinit var wakelockIntent: Intent

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("1", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationbanner(){//Sample Notification that's not being used
        var builder = NotificationCompat.Builder(this, "1")
            .setSmallIcon(R.drawable.roundedbutton)
            .setContentTitle("My notification")
            .setContentText("Much longer text that cannot fit one line...")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Much longer text that cannot fit one line..."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)


        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(0, builder.build())
        }
    }


    private fun leaveNotification(){//Notification that will be shown when people have been on bus for a long time
        var builder = NotificationCompat.Builder(this, "1")
            .setSmallIcon(R.drawable.roundedbutton)
            .setContentTitle("Leave Shuttle?")
            .setContentText("Hey, we noticed that you've been contributing for a while now." +
                    " Have you left the bus yet?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)


        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(0, builder.build())
        }

        // Still need to add action button to actually leave the bus
    }


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

        createNotificationChannel()
        fabBGLayout.setOnClickListener { closeFABMenu() }
        //button variable initiation
        var btn_announcements = findViewById(R.id.fabLayout5) as LinearLayout
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

        wakelockIntent = Intent(this, Wakelock::class.java)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)//This "this" means the location is only being updated when MapActivity is active
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5 * 1000 // refreshes every 5 seconds
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
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )





        //btn_announcements.isVisible = true

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
        //button reference for announcements section
        btn_announcements.setOnClickListener {
            val intent = Intent(this, AnnouncementsActivity::class.java)
            startActivity(intent);
        }

        boardBusButton.setOnClickListener {
            /**
             *  1. get available bus numbers from server
             *  2. start a alert dialog to let user choose which bus to board
             *  3. send data to server
             *  4. update this client's state and change the button to "leave bus"
             */

            val notificationTimer = Timer("notificationTimer", true)

            // 18 minutes after they board we check if they are still on the bus
            notificationTimer.schedule(1080000){
                if(boardBusButton.visibility == View.GONE) {
                    leaveNotification()
                }
            }

            // Check if the user is near a bus stop. If not, pop up an alert dialog and stop this button's onclick listener.
            if (!checkNearbyStop()) {
                return@setOnClickListener
            }
            val busNumberArray = getAvailableBusNumbers().sorted().map { it.toString() }
                .toTypedArray() // convert Array<Int> to Array<String>
            if (internet_connection()) {
                // Given an array of bus numbers, create an AlertDialog to let the user choose which bus to board.
                val chooseBusDialogBuilder = AlertDialog.Builder(this)
                chooseBusDialogBuilder.setTitle("Bus Selection")

                    // TODO: Find closest bus and recommend this bus to the user.

                    .setSingleChoiceItems(busNumberArray, -1) { _, which ->
                        selectedBusNumber = busNumberArray[which]
                    }
                    .setPositiveButton("Continue") { dialog, _ ->
                        if (selectedBusNumber != null) {
                            startForegroundService(wakelockIntent)
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

            ;
            leaveBusButton.setOnClickListener {
                stopService(wakelockIntent)
                onBus = false // this variable controls when the data-transmitting thread ends
                boardBusButton.visibility = View.VISIBLE
                leaveBusButton.visibility = View.GONE
            };
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        }else{
                offline_check()
            }
        }
    }

    /**
     *  Run the is-near-any-stop check. If user is not near any stop, pop up an AlertDialog.
     *  @return true if the user is within 20 meters of any stop; otherwise false.
     */
    private fun checkNearbyStop(): Boolean {
        return if (!isNearStop()) {
            val noNearbyStopDialogBuilder = AlertDialog.Builder(this)
            val noNearbyStopMessage = "You can't board a bus if you're not within 50 meters of a stop."
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
     *  Checks if this user is within 50 meters of any bus stop.
     */
    private fun isNearStop(): Boolean {
        //updateCurrentLocation()
        for (stop in stopArray) {
            val stopLocation = Location("stop location")
            stopLocation.latitude = stop.latitude
            stopLocation.longitude = stop.longitude
            println("current location: $currentLocation") // // TODO: remove/comment this testing clause
            println("stop location: $stopLocation") // // TODO: remove/comment this testing clause
            if (currentLocation?.distanceTo(stopLocation)!! <= 50) {
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
                try
                {
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
                        val sharedPreferences: SharedPreferences =
                            getSharedPreferences("preferences", Context.MODE_PRIVATE)
                        val server_url = sharedPreferences.getString("server_base_url", resources.getString(R.string.default_server_url))
                        val boardBusUrl =
                            URL(server_url + resources.getString(R.string.buses_url) + "/$selectedBusNumber")
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
                catch(ex: Exception)
                {
                    runOnUiThread{offline_check()}
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
                try {
                    val sharedPreferences: SharedPreferences =
                        getSharedPreferences("preferences", Context.MODE_PRIVATE)
                    val server_url = sharedPreferences.getString("server_base_url", resources.getString(R.string.default_server_url))
                    val url = URL(server_url + resources.getString(R.string.bus_numbers_url))
                    val jsonString = url.readText()
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        busNumberArray.add(jsonArray.getInt(i))
                    }
                }
                catch (ex: Exception)
                {
                    runOnUiThread{offline_check()}
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
        fabLayout5.visibility = View.VISIBLE
        fabBGLayout.visibility = View.VISIBLE
        fab.animate().rotationBy(180F)
        fabLayout1.animate().translationY(-resources.getDimension(R.dimen.standard_75))
        fabLayout2.animate().translationY(-resources.getDimension(R.dimen.standard_135))
        fabLayout3.animate().translationY(-resources.getDimension(R.dimen.standard_215))
        fabLayout4.animate().translationY(-resources.getDimension(R.dimen.standard_210))
        //fabLayout5.animate().translationY(-resources.getDimension(R.dimen.standard_12))
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
        //fabLayout5.animate().translationY(0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {}
                override fun onAnimationEnd(animator: Animator) {
                    if (View.GONE == fabBGLayout.visibility) {
                        fabLayout1.visibility = View.GONE
                        fabLayout2.visibility = View.GONE
                        fabLayout3.visibility = View.GONE
                        //fabLayout5.visibility = View.GONE
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
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val server_url = sharedPreferences.getString("server_base_url", res.getString(R.string.default_server_url))
        if(internet_connection())
        {
            busMarkerArray = if(!busesDrawn) {
                 drawBuses(server_url + res.getString(R.string.buses_url))
            } else{
                 updateBuses(server_url + res.getString(R.string.buses_url), busMarkerArray)
            }
        }
    }

    fun drawStops(url: String) : ArrayList<Stop> {
        val stopArray = ArrayList<Stop>()
        val thread = Thread(Runnable {
            kotlin.run {
                try {
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
                catch(ex: Exception)
                {
                    runOnUiThread{offline_check()}
                }
            }
        })
        thread.start()
        return stopArray;
    }
    fun drawRoutes(url: String) {
        val thread2 = Thread(Runnable {
            kotlin.run {
                try {
                    val url = URL(url)
                    val jsonString = url.readText()
                    var jsonArray = JSONArray(jsonString)
                    var latlngmultiroutes = ArrayList<ArrayList<LatLng>>()
                    var routeColorArr = ArrayList<String>()
                    for (i in 0 until jsonArray.length()) {
                        var routeObject = jsonArray.getJSONObject(i)
                        var coordArray = routeObject.getJSONArray("coordinates")
                        var latlngarr = ArrayList<LatLng>()
                        routeColorArr.add(routeObject.getString("colorName"))
                        for (i in 0 until coordArray.length()) {
                            val waypoint = coordArray.getJSONObject(i)
                            val latitude = waypoint.getDouble("latitude")
                            val longitude = waypoint.getDouble("longitude")
                            val latlng = LatLng(latitude, longitude)
                            latlngarr.add(latlng)
                        }
                        latlngmultiroutes.add(latlngarr)
                    }
                    runOnUiThread {
                        val colorArr = arrayListOf<Pair<String, Int>>(
                            Pair("red", Color.RED),
                            Pair("orange", Color.parseColor("#ee6002")),
                            Pair("yellow", Color.YELLOW),
                            Pair("green", Color.GREEN),
                            Pair("blue", Color.BLUE),
                            Pair("purple", Color.parseColor("#a200e0")),
                            Pair("pink", Color.parseColor("#ef4fa6")),
                            Pair("gray", Color.GRAY),
                        )
                        var polylineArr = ArrayList<Polyline>()
                        for(i in 0 until latlngmultiroutes.size) {
                            var color : Int = 0
                            var flag = false
                            for(j in 0 until colorArr.size) {
                                if(colorArr[j].first.equals(routeColorArr[i])) {
                                    color = colorArr[j].second
                                    flag = true
                                    break
                                }
                            }
                            if(!flag) {
                                color = Color.BLACK
                            }
                            polylineArr.add(mMap.addPolyline(
                                PolylineOptions()
                                    .clickable(true)
                                    .addAll(latlngmultiroutes[i])
                                    .color(color)
                                    .width(4F)
                            ))
                        }
                    }
                }
                catch(ex: Exception)
                {
                    runOnUiThread{offline_check()}
                }
            }
        })
        thread2.start()
        routeDrawn = true
    }

    //@RequiresApi(Build.VERSION_CODES.O)
    fun drawBuses(url: String): ArrayList<Marker> {
        //val busArray = ArrayList<Bus>()
        var markerArray = ArrayList<Marker>()
        val thread = Thread(Runnable {
            kotlin.run {
                try {
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
                        if (colorblindMode.getMode()) {
                            if (busType == "user") {
                                busIcon = getString(R.string.colorblind_crowdsourced_bus)
                            } else {
                                busIcon = getString(R.string.colorblind_GPS_bus)
                            }
                        } else {
                            if (busType == "user") {
                                busIcon = getString(R.string.crowdsourced_bus)
                            }
                        }

                        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        val busDate: LocalDateTime = LocalDateTime.parse(
                            date,
                            format//DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        )
                        val busObject = Bus(latitude, longitude, id, busIcon, busDate.toString())
                        val tmp = busDate.toString()
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
                                    ).zIndex(1F).snippet("0 seconds ago")

                                )
                            )
                            markerArray.get(i).tag = current.busDate;
                        }
                    }
                }
                catch(ex: Exception)
                {
                    runOnUiThread{offline_check()}
                }
            }
        })
        thread.start()
        busesDrawn = true
        return markerArray
    }
    //@RequiresApi(Build.VERSION_CODES.O)
    fun updateBuses(url: String, markerArray: ArrayList<Marker>): ArrayList<Marker> {
        if (markerArray.size == 0 && !busesDrawn) {
            return markerArray
        }
        val busArray = ArrayList<Bus>()
        //var markerArray = ArrayList<Marker>()
        val thread = Thread(Runnable {
            kotlin.run {
                try {
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
                        if (colorblindMode.getMode()) {
                            if (busType == "user") {
                                busIcon = getString(R.string.colorblind_crowdsourced_bus)
                            } else {
                                busIcon = getString(R.string.colorblind_GPS_bus)
                            }
                        } else {
                            if (busType == "user") {
                                busIcon = getString(R.string.crowdsourced_bus)
                            }
                        }

                        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        val busDate = LocalDateTime.parse(date, format)
                        val busObject = Bus(latitude, longitude, id, busIcon, busDate.toString())
                        var found = false
                        for (i in 0 until markerArray.size) {
                            runOnUiThread {
                                if (markerArray.get(i).tag != null && markerArray.get(i).tag!!.equals(
                                        id
                                    )
                                ) {
                                    found = true
                                    markerArray.get(i).setPosition(LatLng(latitude, longitude))
                                    markerArray.get(i)
                                        .setIcon(BitmapDescriptorFactory.fromAsset(busIcon))
                                    //println("Bus " + id + " updated.")
                                }
                            }
                            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            val busDate = LocalDateTime.parse(date, format)
                            val busObject =
                                Bus(latitude, longitude, id, busIcon, busDate.toString())
                            var found = false
                            for (i in 0 until markerArray.size) {
                                runOnUiThread {
                                    var len = markerArray.get(i).title.length
                                    var busID = markerArray.get(i).title.substring(4, len).toInt()
                                    if (busID!!.equals(id)) {
                                        found = true
                                        markerArray.get(i).setPosition(LatLng(latitude, longitude))
                                        markerArray.get(i)
                                            .setIcon(BitmapDescriptorFactory.fromAsset(busIcon))
                                        //println("Bus " + id + " updated.")
                                        markerArray.get(i).tag = busDate
                                    }
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
                                        ).zIndex(1F).snippet("0 seconds ago")
                                    )
                                )
                                markerArray.get(i).tag = (current.busDate)
                                //println(current.busDate)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    runOnUiThread { offline_check() }
                }
            }
        })
        thread.start()
        return markerArray
    }

    fun updateApp(){
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=edu.rpi.shuttletracker")
        )
        startActivity(browserIntent)
        finish()
    }

    fun promptDownload(){
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Outdated App")
            .setMessage("Your app is outdated and no longer works.")
            .setPositiveButton("Update") { _, _ ->
                updateApp()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setOnCancelListener(DialogInterface.OnCancelListener(){finish()})

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    fun APIPull(result: Data<Int>, website: String): Int {
        val thread = Thread(Runnable {
            kotlin.run {
                val url = URL(website)
                val data = url.readText()
                result.value = data.toInt()
            }
        })
        thread.start()
        return result.value
    }
    fun APIVersionMatch(website: String, apikey : Int): Boolean {
        val data = Data<Int>(-1)
        APIPull(data, website)
        while(data.value==-1){}
        return data.value == apikey

    }

    fun internet_connection(): Boolean {
        //Check if connected to internet, output accordingly
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val networkCapabilities = cm.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    fun updateMarker(busMarkerArrayArgument: ArrayList<Marker>){
                for (markeri in busMarkerArrayArgument) {
                    val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
                    //search time in busarray until id matches.
                    if (markeri.tag == null) {
                        continue
                    }
                    var dateTag = markeri.tag.toString()//TODO: Try catch, parisng null marker
                    //println(dateTag)
                    val busDate = LocalDateTime.parse(dateTag)
                    val seconds: Long = ChronoUnit.SECONDS.between(busDate, currentDate)
                    val minutes: Long = ChronoUnit.MINUTES.between(busDate, currentDate)
                    val hours: Long = ChronoUnit.HOURS.between(busDate, currentDate)
                    val days: Long = ChronoUnit.DAYS.between(busDate, currentDate)
                    if (days == 0.toLong() && hours == 0.toLong() && minutes == 0.toLong()) {
                        markeri.snippet = "$seconds seconds ago"
                    } else if (days == 0.toLong() && hours == 0.toLong()) {
                        if (minutes == 1.toLong()) {
                            markeri.snippet = "$minutes minute ago"
                        } else {
                            markeri.snippet = "$minutes minutes ago"
                        }
                    }
                    if(markeri.isInfoWindowShown) {
                        markeri.showInfoWindow()
                    }
                }

    }

    fun offline_check(){
        if(!alertPrompted) {
            alertPrompted=true
            AlertDialog.Builder(this).setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    alertPrompted=false
                }
                .setIcon(android.R.drawable.ic_dialog_alert).show()
                .setOnCancelListener(DialogInterface.OnCancelListener() { alertPrompted=false })
        }
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
        val boardBusButton = findViewById<Button>(R.id.board_bus_button)
        val leaveBusButton = findViewById<Button>(R.id.leave_bus_button)

        val res: Resources = getResources()

        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val server_url = sharedPreferences.getString("server_base_url", res.getString(R.string.default_server_url))

        mMap = googleMap
        mMap.getUiSettings().setMapToolbarEnabled(false)
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {} // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {
                googleMap.setMapStyle(
                    MapStyleOptions(
                        getResources()
                            .getString(R.string.style_json)
                    )
                );
            } // Night mode is active, we're using dark theme
        }
        val Union = LatLng(42.730426, -73.676573)
        mMap.setMinZoomPreference(13.5f)
        mMap.setMaxZoomPreference(20.0f)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Union))

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

        if (!internet_connection()) {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again")
                .setPositiveButton("Restart") { _, _ ->
                    finish()//TODO:change restart to retry
                    startActivity(intent)

                }
                .setNeutralButton("Close") { _, _ ->
                    finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setOnCancelListener(DialogInterface.OnCancelListener() { finish() })

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        } else {
            //Internet connection confirmed, matching API
            APImatch = APIVersionMatch(
                server_url + res.getString(R.string.version_url),
                res.getInteger(R.integer.api_key)
            )
            if (!APImatch) {
                promptDownload()
                //runOnUiThread { promptDownload() }
            }
        }
//        val sharedPreferences: SharedPreferences =
//            this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        if (sharedPreferences.contains("toggle_value")) {
            MapsActivity.colorblindMode.setMode(sharedPreferences.getBoolean("toggle_value", true))
        }
        if (internet_connection() && APImatch) {//TODO:make sure the stops and routes are only draw once

            stopArray = drawStops(server_url + res.getString(R.string.stops_url))
            drawRoutes(server_url + res.getString(R.string.routes_url))
        }
        val busTimer = Timer("busTimer", true)
        val markerTimer = Timer("markerTimer",true)

        if (APImatch){
            if (!busesDrawn) {//TODO: bandage for now
                busMarkerArray = drawBuses(server_url + res.getString(R.string.buses_url))
            }
        }

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true)
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.alertTitle))
                .setMessage(resources.getString(R.string.supporting_text))
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                    // Respond to negative button press
                    mMap.setMyLocationEnabled(false)
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> startActivity(
                            Intent(
                                ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                            )
                        )

                        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                ACCESS_FINE_LOCATION,
                                ACCESS_BACKGROUND_LOCATION
                            ),
                            MY_PERMISSIONS_REQUEST_LOCATION
                        )

                        else -> ActivityCompat.requestPermissions(
                            this,
                            arrayOf(ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_LOCATION
                        )

                    }
                }
                .show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(ContextCompat.checkSelfPermission(this, BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(resources.getString(R.string.bluetooth_permissions_title))
                    .setMessage(resources.getString(R.string.bluetooth_supporting_text))
                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                        // Respond to negative button press
                        mMap.setMyLocationEnabled(false)
                    }
                    .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                BLUETOOTH_SCAN,
                                BLUETOOTH_CONNECT
                            ),
                            MY_PERMISSIONS_REQUEST_BLUETOOTH
                        )
                    }
                    .show()
            }
        }

        busTimer.scheduleAtFixedRate(0, 5000) {
            //if(APIMatch)
            if(internet_connection() && APImatch) {//make sure it would run only when connected to internet and after api check
                busMarkerArray = updateBuses(server_url + res.getString(R.string.buses_url), busMarkerArray)
                if(!routeDrawn){
                    stopArray = drawStops(server_url + res.getString(R.string.stops_url))
                    drawRoutes(server_url + res.getString(R.string.routes_url))
                }
            }
            //println("Updated bus locations.")
        }
        markerTimer.scheduleAtFixedRate(0,1000){

            println("Current location is ")
            println(currentLocation)
            if(busesDrawn) {
                runOnUiThread { updateMarker(busMarkerArray) }
            }
            if(!internet_connection()&&onBus){


                onBus = false // this variable controls when the data-transmitting thread ends
                runOnUiThread() {
                    boardBusButton.visibility = View.VISIBLE
                    leaveBusButton.visibility = View.GONE
                }
            }
        }

        var btn_refresh = findViewById(R.id.fab4) as FloatingActionButton
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        btn_refresh.animation = rotate
        btn_refresh.setOnClickListener {
            if(internet_connection()) {
                //recheck apikey
                    if (!APImatch){//safty check
                        promptDownload()
                    }
                if(!routeDrawn){
                    stopArray = drawStops(server_url + res.getString(R.string.stops_url))
                    drawRoutes(server_url + res.getString(R.string.routes_url))
                }
                btn_refresh.startAnimation(rotate)
                Toast.makeText(applicationContext, "Refreshed!", Toast.LENGTH_SHORT).show()
                busMarkerArray = updateBuses(server_url + res.getString(R.string.buses_url), busMarkerArray)
            }else {
                offline_check()
            }
        }
    }

    // Store bus date in marker
    // Store bus date in separate bus object maintained in separate bus object array alongside marker array
    // Change to list of pairs between markers and strings

/*
    override fun onMarkerClick(marker: Marker): Boolean {
        /*
            Save the date to each marker as a tag in the draw/update buses methods.
            Parse this date in this function, compare it to the current date, and
            display how long it was ago as this text. We can achieve this similarly to
            how date comparison to ensure the bus data is less than 5 mins old in the
            draw/update buses methods.
         */
        val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
        println("Current Time $currentDate")
        //Get the ID from marker

        /*
        var len=marker.snippet.length
        if (len>0 && marker.snippet.substring(len-3, len) == "ago") {
            return false
        }
         */

        //search time in busarray until id matches.
        var dateTag=marker.tag.toString()//TODO: Try catch, parisng null marker DateTimeParseException
        println(dateTag)
        val busDate = LocalDateTime.parse(dateTag)
        val seconds: Long = ChronoUnit.SECONDS.between(busDate, currentDate)
        val minutes: Long = ChronoUnit.MINUTES.between(busDate, currentDate)
        val hours: Long = ChronoUnit.HOURS.between(busDate, currentDate)
        val days: Long = ChronoUnit.DAYS.between(busDate, currentDate)
        if (days == 0.toLong() && hours == 0.toLong() && minutes == 0.toLong() ) {
            marker.snippet = "$seconds seconds ago"
        } else if (days == 0.toLong() && hours == 0.toLong()){
            if (minutes == 1.toLong()) {
                marker.snippet = "$minutes minute ago"
            } else {
                marker.snippet = "$minutes minutes ago"
            }
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }
*/

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
                        //println("current location updated") // TODO: remove/comment this testing clause
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
        private const val MY_PERMISSIONS_REQUEST_BLUETOOTH = 98
    }
}
