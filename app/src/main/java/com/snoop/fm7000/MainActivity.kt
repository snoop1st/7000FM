package com.snoop.fm7000

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snoop.fm7000.ui.theme.DynamicMaterial3Theme
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the MediaService when the app opens
        val intent = Intent(this, MediaService::class.java)
        startService(intent)

        setContent {
            DynamicMaterial3Theme {
                HomeScreen(fetchTrack = ::fetchCurrentTrack)
            }
        }
    }

    private fun fetchCurrentTrack(callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://stream.7000fm.gr/")
                    .build()
                val response = client.newCall(request).execute()
                val html = response.body?.string()
                if (html != null) {
                    val document = Jsoup.parse(html)
                    val trackElement = document.selectFirst("#current-track")
                    withContext(Dispatchers.Main) {
                        callback(trackElement?.text())
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
}
