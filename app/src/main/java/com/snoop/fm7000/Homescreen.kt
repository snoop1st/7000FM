package com.snoop.fm7000

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snoop.fm7000.ui.theme.DynamicMaterial3Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

// ✅ Use MutableStateFlow for state updates across recompositions
private val isPlayingState = MutableStateFlow(false)

@Composable
fun HomeScreen(fetchTrack: (callback: (String?) -> Unit) -> Unit) {
    val context = LocalContext.current
    var currentTrack by remember { mutableStateOf("Fetching track...") }
    val isPlaying by isPlayingState.collectAsState() // Observe the playback state

    // Fetch track info when UI appears
    LaunchedEffect(Unit) {
        fetchTrack { track ->
            currentTrack = track ?: "Track not found"
        }
    }

    // ✅ BroadcastReceiver to listen for playback state updates
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val newState = intent?.getBooleanExtra("isPlaying", false) ?: false
                Log.d("HomeScreen", "Received playback state: $newState")
                isPlayingState.value = newState // ✅ Update MutableStateFlow
            }
        }

        val filter = IntentFilter("com.snoop.fm7000.PLAYBACK_STATE_CHANGED")
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Album art image
                Image(
                    painter = painterResource(id = R.drawable.fm7000_for_home),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Display current track title
                Text(
                    text = currentTrack,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Play/Pause button
                IconButton(
                    onClick = {
                        val action = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
                        sendActionToService(context, action)
                        Log.d("HomeScreen", "Button clicked: $action")
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    )
}

// ✅ Function to send play/pause actions to MediaService
fun sendActionToService(context: Context, action: String) {
    val intent = Intent(context, MediaService::class.java).apply {
        this.action = action
    }
    ContextCompat.startForegroundService(context, intent)
}
