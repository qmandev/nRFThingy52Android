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
import com.armstrongmobile.nrfthingy52android.ui.theme.ThingyTheme

// Phase 1 placeholder: the bare MaterialTheme is replaced with ThingyTheme (Nordic palette). Phase 5
// replaces this content with the scanner screen and a NavHost. Single-Activity Compose entry point
// (plan §3). The placeholder text is tinted with the theme primary (nordicBlue) purely to make the
// palette visible while the app has no real UI yet.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThingyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        text = "Thingy52",
                        color = MaterialTheme.colorScheme.primary,
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
    ThingyTheme {
        Text(text = "Thingy52", color = MaterialTheme.colorScheme.primary)
    }
}
