package edu.rpi.shuttletracker
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.android.synthetic.main.activity_announcements.*
import org.json.JSONArray
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class AnnouncementsActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcements)
        val toolbar: Toolbar = findViewById(R.id.announcementsToolbar)
        val textView: View = findViewById(R.id.announcementsTextView)
        transparentStatusAndNavigation()
        val thread = Thread(kotlinx.coroutines.Runnable {
            kotlin.run {
                val sharedPreferences: SharedPreferences =
                    getSharedPreferences("preferences", Context.MODE_PRIVATE)
                val server_url = sharedPreferences.getString("server_base_url", resources.getString(R.string.default_server_url))
                val announ = URL(server_url + resources.getString(R.string.announcements_url))
                var announString = announ.readText()
                val announArray = JSONArray(announString)
                val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
                val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

                announString = ""
                for (i in 0 until announArray.length()) {
                    val sched_type = announArray.getJSONObject(i).getString("scheduleType")
                    val start = announArray.getJSONObject(i).getString("start")
                    val end = announArray.getJSONObject(i).getString("end")
                    val startDate = LocalDateTime.parse(start, format)
                    val endDate = LocalDateTime.parse(end, format)
                    if((currentDate>=startDate && currentDate<=endDate) || (sched_type =="none") ||
                        (sched_type =="startOnly" && currentDate>=startDate) ||
                        (sched_type =="endOnly" && currentDate<=endDate))
                        announString += startDate.toString().substringBefore('T') + ":\n" +
                                "____________"+ "\n" +
                                announArray.getJSONObject(i).getString("body") + "\n" + "\n\n\n"
                }
                if(announString.length>0)
                    announcementsTextView.text = announString
            }
        })
        thread.start()
        thread.join()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v: View, insets: WindowInsetsCompat ->
            (v.layoutParams as ViewGroup.MarginLayoutParams).topMargin = insets.systemWindowInsetTop
            insets.consumeSystemWindowInsets()
        }
        ViewCompat.setOnApplyWindowInsetsListener(textView) { v: View, insets: WindowInsetsCompat ->
            (v.layoutParams as ViewGroup.MarginLayoutParams).topMargin = insets.systemWindowInsetTop
            insets.consumeSystemWindowInsets()
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = insets.systemWindowInsetBottom
            insets.consumeSystemWindowInsets()
        }
    }

    private fun transparentStatusAndNavigation() {
        //make full transparent statusBar
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, true
            )
        }
        if (Build.VERSION.SDK_INT >= 19) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false
            )
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win = window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
                // handle arrow click here
                if (item.itemId == android.R.id.home) {
                    finish() // close this activity and return to preview activity (if there is any)
                }
        return super.onOptionsItemSelected(item)
    }
}