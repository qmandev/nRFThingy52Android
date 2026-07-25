package com.armstrongmobile.nrfthingy52android.ui.scanner

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.armstrongmobile.nrfthingy52android.R
import com.armstrongmobile.nrfthingy52android.domain.RssiBucket
import com.armstrongmobile.nrfthingy52android.ui.theme.ThingyTheme

// One scanner list row: name on the left, RSSI icon on the right — the port of iOS's ThingyRowView
// (HStack { Text; Spacer; Image(38x38) }).
@Composable
fun ThingyRow(
    thingy: DiscoveredThingyUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = thingy.name, style = MaterialTheme.typography.bodyLarge)
        Icon(
            painter = painterResource(thingy.rssiBucket.drawableRes),
            contentDescription = stringResource(R.string.cd_signal_strength, thingy.rssiBucket.tier),
            modifier = Modifier.size(38.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Maps the domain bucket to its drawable. The mapping lives here, in the UI layer, so the domain
// enum stays free of Android resource ids (plan §9.2's note on adapting testImageNamesMatchAssets).
@get:DrawableRes
val RssiBucket.drawableRes: Int
    get() = when (this) {
        RssiBucket.WEAKEST -> R.drawable.rssi_1
        RssiBucket.WEAK -> R.drawable.rssi_2
        RssiBucket.MEDIUM -> R.drawable.rssi_3
        RssiBucket.STRONG -> R.drawable.rssi_4
    }

@Preview(showBackground = true)
@Composable
private fun ThingyRowPreview() {
    ThingyTheme {
        ThingyRow(
            thingy = DiscoveredThingyUi(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Thingy52 Mock",
                rssiBucket = RssiBucket.MEDIUM,
                lastUpdated = 0L,
            ),
            onClick = {},
        )
    }
}
