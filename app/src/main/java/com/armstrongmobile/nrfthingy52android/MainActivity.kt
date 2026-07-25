package com.armstrongmobile.nrfthingy52android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// Phase 0 placeholder. Phase 1 introduces ThingyTheme; Phase 5 replaces this content with the
// scanner screen and a NavHost. Single-Activity Compose entry point (plan §3).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        text = "Thingy52",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    MaterialTheme {
        Text(text = "Thingy52")
    }
}
