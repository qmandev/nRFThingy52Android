package com.armstrongmobile.nrfthingy52android.ui.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// One dashboard row: tinted icon, label, and the reading on the trailing edge — the port of the iOS
// view's sensorRow(symbol:label:value:) helper (plan §6.2).
@Composable
fun SensorRow(
    @DrawableRes icon: Int,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            text = value ?: SensorFormat.PLACEHOLDER,
            // The weight(1f) spacer above collapses to nothing once a value is wide enough to claim
            // the rest of the row, which left ru "Альбомная (перевёрнутая)" touching its label with no
            // gap at all. This guarantees the separation regardless of value width.
            modifier = Modifier.padding(start = 16.dp),
            // Tabular figures keep the values from jittering as they stream in — Compose's
            // equivalent of SwiftUI's .monospacedDigit().
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Only matters once a value is long enough to wrap, which no reading did until the
            // orientation labels were localized: ru "Альбомная (перевёрнутая)" takes two lines. Without
            // this the wrapped lines fall back to start-alignment and sit left of every single-line
            // value in the column. iOS wraps these right-aligned, so this keeps the two in step.
            textAlign = TextAlign.End,
        )
    }
}
