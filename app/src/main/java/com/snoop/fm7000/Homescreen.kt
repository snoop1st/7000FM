package com.snoop.fm7000

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.snoop.fm7000.ui.theme.DynamicMaterial3Theme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    mediaPlayer: MediaPlayer,
    fetchTrack: (callback: (String?) -> Unit) -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentTrack by remember { mutableStateOf("Fetching track...") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Check and request notification permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as MainActivity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                MainActivity.NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, start the service
            MusicService.startService(context)
        }
    } else {
        // Permission not required, start the service
        MusicService.startService(context)
    }

    // Fetch the current track when the composable is displayed
    LaunchedEffect(Unit) {
        fetchTrack { track ->
            currentTrack = track ?: "Track not found"
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
                // Replace the CircleShape with the static image
                Image(
                    painter = painterResource(id = R.drawable.fm7000_for_home), // Reference to your image file
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(200.dp) // Size of the image
                        .clip(CircleShape), // Clip the image into a circle
                    contentScale = ContentScale.Crop // Scale the image appropriately
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current Track Display
                Text(
                    text = currentTrack,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Play/Pause Button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer.pause()
                        } else {
                            mediaPlayer.start()
                        }
                        isPlaying = !isPlaying
                        Toast.makeText(context, "Play state: $isPlaying", Toast.LENGTH_SHORT).show()
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DynamicMaterial3Theme {
        HomeScreen(
            mediaPlayer = MediaPlayer(),
            fetchTrack = { callback -> callback("Track Name - Artist") }
        )
    }
}