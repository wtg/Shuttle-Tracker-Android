package edu.rpi.shuttletracker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import edu.rpi.shuttletracker.MapsActivity.colorblindMode.getSharedPreferences
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URL
import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.*

object Logs {

    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private var logBuffer = ArrayList<String>()

    fun sendLogsToServer(logsURL: URL) {

        Log.d("log_save", "target logs url: $logsURL")
        trimLogsBuffer()

        val logJSONObject = createLogMessage(logBuffer.toList().joinToString(separator = ""))
        Log.d("log_save", "log json server: " + logJSONObject.toString())

        // send to server
        Thread {
            kotlin.run {
                try {
                    val request = Request.Builder()
                        .url(logsURL)
                        .post(
                            logJSONObject.toString().toRequestBody(mediaType)
                        )
                        .build()
                    Log.d("log_save", "logs request: $request")

                    val response = httpClient.newCall(request).execute()
                    Log.d("log_save", "logs server response: " + response.body?.string() )

                } catch (ex: Exception) {
                    writeToLogBuffer(object{}.javaClass.enclosingMethod.name,ex.toString())
                    Log.d("log_save", "server send failed: " + ex.toString())
                }
                flushLogBuffer()
            }
        }.start()

    }

    private fun createLogMessage(message: String): JSONObject {
        val session_uuid = UUID.randomUUID().toString()
        val date = getCurrentFormattedDate()

        val jsonMap = mapOf("id" to session_uuid, "content" to message, "clientPlatform" to "android", "date" to date)
        return JSONObject(jsonMap)
    }

    fun writeToLogBuffer(functionName: Any, message: String){
        val currTime = getCurrentFormattedDate()
        try {
            logBuffer.add("[$currTime] [$functionName] $message \n")
        } catch (ex : Exception) {
            logBuffer.add("[$currTime] [error adding function name] $message \n")
        }
        trimLogsBuffer()
    }

    // overloaded function, in case there is no parent function name
    fun writeToLogBuffer(message: String){
        val currTime = getCurrentFormattedDate()
        logBuffer.add("[$currTime] $message \n")
        trimLogsBuffer()
    }

    fun writeExceptionToLogBuffer(functionName: Any, ex: Exception){
        val currTime = getCurrentFormattedDate()
        logBuffer.add("[$currTime] $functionName " + Log.getStackTraceString(ex) + " \n")
        logBuffer.add("[$currTime] $functionName " + ex.toString() + " \n")
        trimLogsBuffer()
    }

    private fun trimLogsBuffer(){
        // we only need to store the last x amount of log data
        val maxLogSize = 100
        if (logBuffer.size > maxLogSize){
            val removeIndex = logBuffer.size - maxLogSize
            logBuffer.subList(0, removeIndex).clear()
        }
    }

    private fun flushLogBuffer(){
        logBuffer.clear()
    }

    private fun getCurrentFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC") // use UTC as default time zone

        return sdf.format(Date())
    }

//    private fun saveLogsToFile(logJSON: JSONObject){
//        Log.d("log_save", "in save file function")
//        val filename = getRandomSessionUuid()
//        try {
//            val filecontents = logJSON.toString()
//            openFileOutput(filename, Context.MODE_PRIVATE).use {
//                it.write(filecontents.toByteArray())
//                Log.d("log_save", "wrote to file")
//            }
//        } catch (e: Exception){
//            Log.d("log_save", "File write failed: " + e.toString());
//        }
//        readLogs(filename)
//    }
//
//    private fun readLogs(filename: String){
//        Log.d("log_save", "in read file function")
//        try {
//            val `in`: FileInputStream = openFileInput(filename)
//            val inputStreamReader = InputStreamReader(`in`)
//            val bufferedReader = BufferedReader(inputStreamReader)
//            val sb = StringBuilder()
//            var line: String?
//            while (bufferedReader.readLine().also { line = it } != null) {
//                sb.append(line)
//            }
//            Log.d("log_save", "read content: " + sb)
//            inputStreamReader.close()
//        } catch (e: Exception) {
//            Log.e("Exception", "File read failed: " + e.toString());
//        }
//    }


    //    fun createLog(message: String) {
//        val sharedPreferences: SharedPreferences =
//            this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
//        val logJSON = createLogMessage(message)
//
//        saveLogsToFile(logJSON)
//
//        if (sharedPreferences.getBoolean("logs_toggle_value", true)) {
//            Log.d("log_save", "called sendLogToServer")
//            sendLogToServer(logJSON)
//        }
//
//    }


}