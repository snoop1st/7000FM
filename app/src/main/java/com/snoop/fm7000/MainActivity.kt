package com.snoop.fm7000

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.snoop.fm7000.ui.theme.DynamicMaterial3Theme
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicMaterial3Theme {
                HomeScreen(
                    mediaPlayer = mediaPlayer,
                    fetchTrack = ::fetchCurrentTrack
                )
            }
        }

        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource("https://stream.7000fm.gr/radio/8000/radio.mp3")
            mediaPlayer.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load stream", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                MusicService.startService(this)
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
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

    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }
}