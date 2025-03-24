package com.snoop.fm7000

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

private val isPlayingState = MutableStateFlow(false)

@Composable
fun HomeScreen(fetchTrack: (callback: (String?) -> Unit) -> Unit) {
    val context = LocalContext.current
    var currentTrack by remember { mutableStateOf("Fetching track...") }
    val isPlaying by isPlayingState.collectAsState()

    LaunchedEffect(Unit) {
        fetchTrack { track ->
            currentTrack = track ?: "Track not found"
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val newState = intent?.getBooleanExtra("isPlaying", false) ?: false
                Log.d("HomeScreen", "Broadcast received: isPlaying = $newState")
                isPlayingState.value = newState
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
                Image(
                    painter = painterResource(id = R.drawable.fm7000_for_home),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTrack,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))

                IconButton(
                    onClick = {
                        val newAction = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
                        sendActionToService(context, newAction)
                        isPlayingState.value = !isPlaying // Update the state
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    )
}

fun sendActionToService(context: Context, action: String) {
    Log.d("HomeScreen", "📤 Sending Intent: $action")
    val intent = Intent(context, MediaService::class.java).apply {
        this.action = action
    }
    context.startService(intent)
}