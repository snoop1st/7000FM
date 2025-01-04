package com.snoop.fm7000

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snoop.fm7000.ui.theme.DynamicMaterial3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DynamicMaterial3Theme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    var isPlaying by remember { mutableStateOf(true) } // Track whether music is playing
    val songName = "Track Name"
    val artist = "Artist Name"
    val context = LocalContext.current // Get the context for Toast

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
                // Album Art Placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Gray, CircleShape) // Placeholder for album art
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Song Title and Artist
                Text(
                    text = "$songName - $artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Play/Pause Circular Button
                IconButton(
                    onClick = {
                        // Toggle the play/pause state
                        isPlaying = !isPlaying
                        // Show Toast message
                        Toast.makeText(context, "Button Pressed! Play state: $isPlaying", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.ShoppingCart,
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
fun DefaultPreview() {
    DynamicMaterial3Theme {
        HomeScreen()
    }
}
