package com.armstrongmobile.nrfthingy52android.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.armstrongmobile.nrfthingy52android.R
import com.armstrongmobile.nrfthingy52android.domain.RssiBucket
import com.armstrongmobile.nrfthingy52android.ui.theme.NordicColors
import com.armstrongmobile.nrfthingy52android.ui.theme.ThingyTheme

// The scanner screen: nearby Thingys advertising the UI service, a scanning indicator, and an empty
// state — the port of iOS's ScannerView.
//
// Nav-bar treatment (plan §6.1): iOS deliberately avoids an opaque colored bar because on iOS 26 that
// hides SwiftUI's large title, so it tints the Liquid Glass bar instead. Material 3 has no such
// conflict, so the brand intent is expressed directly — a solid Nordic-blue bar with a white title.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    uiState: ScannerUiState,
    onDeviceSelected: (String) -> Unit,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // iOS: .onAppear { clearDiscovered(); startScan() }.
    LaunchedEffect(Unit) { onStartScan() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NordicColors.nordicBlue,
                    scrolledContainerColor = NordicColors.nordicBlue,
                    titleContentColor = Color.White,
                ),
                actions = {
                    // iOS shows a ProgressView in the trailing toolbar slot only while scanning.
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            color = Color.White,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (uiState.discovered.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // iOS: Section("Nearby Devices"), shown only when the list is non-empty.
                    item {
                        Text(
                            text = stringResource(R.string.nearby_devices),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                        )
                    }
                    items(uiState.discovered, key = { it.address }) { thingy ->
                        ThingyRow(thingy = thingy, onClick = { onDeviceSelected(thingy.address) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// Mirrors iOS's ContentUnavailableView: icon + title, then the two instruction lines each followed by
// a smaller caption line. Text is copied verbatim from the string table (plan §6.3).
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_scanning),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.cant_see_your_thingy),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.empty_state_step_1),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.empty_state_step_1_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.empty_state_step_2),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.empty_state_step_2_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerScreenPopulatedPreview() {
    ThingyTheme {
        ScannerScreen(
            uiState = ScannerUiState(
                discovered = listOf(
                    DiscoveredThingyUi("AA:BB:CC:DD:EE:FF", "Thingy52 Mock", RssiBucket.STRONG, 0L),
                    DiscoveredThingyUi("11:22:33:44:55:66", "Unknown Device", RssiBucket.WEAK, 0L),
                ),
                isScanning = true,
            ),
            onDeviceSelected = {},
            onStartScan = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerScreenEmptyPreview() {
    ThingyTheme {
        ScannerScreen(
            uiState = ScannerUiState(isScanning = true),
            onDeviceSelected = {},
            onStartScan = {},
        )
    }
}
