package edu.rpi.shuttletracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.net.URL

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val toolbar: Toolbar = findViewById(R.id.aboutToolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setDisplayShowHomeEnabled(true)
        val button: Button = findViewById(R.id.githubButton)
        val res: Resources = getResources()
        button.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(res.getString(R.string.repo_url)),
            )
            Logs.writeToLogBuffer(
                object {}.javaClass.enclosingMethod.name,
                "opened browser to github link",
            )
            startActivity(browserIntent)
        }

        val privacybutton: Button = findViewById(R.id.privacyButton)
        privacybutton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(res.getString(R.string.privacy_url)),
            )
            Logs.writeToLogBuffer(
                object {}.javaClass.enclosingMethod.name,
                "opened browser to privacy policy",
            )
            startActivity(browserIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

    // for future logging
    private fun getLogsURL(): URL {
        val res: Resources = getResources()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val server_url = sharedPreferences.getString(
            "server_base_url",
            res.getString(R.string.default_server_url),
        )
        val logsUrl =
            URL(server_url + res.getString(R.string.logs_url))
        return logsUrl
    }
}
