package com.armstrongmobile.nrfthingy52android.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.armstrongmobile.nrfthingy52android.R
import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import com.armstrongmobile.nrfthingy52android.ui.theme.NordicColors
import com.armstrongmobile.nrfthingy52android.ui.theme.ThingyTheme

// The Thingy detail screen: LED toggle and live button state — the port of iOS's ThingyDetailView.
// Environment and Motion dashboard sections arrive in Phase 7.
//
// The title bar is the inline/small style here, matching iOS's
// .navigationBarTitleDisplayMode(.inline) on this screen specifically (the scanner uses a large title).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThingyDetailScreen(
    uiState: ThingyDetailUiState,
    onLedToggle: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // iOS: .onAppear { connect() } / .onDisappear { disconnect() }. Compose's onDispose fires after
    // the back transition completes, the same ordering as .onDisappear — the iOS status doc flags
    // quick back-then-reselect as worth a race check, carried onto the Phase 9 checklist (plan §6.2).
    DisposableEffect(Unit) {
        onConnect()
        onDispose { onDisconnect() }
    }

    val heavyImpact = rememberHeavyImpactHaptic()
    // Fires only on the transition into pressed, like iOS's sensoryFeedback trigger predicate.
    LaunchedEffect(uiState.buttonPressed) {
        if (uiState.buttonPressed) heavyImpact()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.name) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NordicColors.nordicBlue,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingsSection(
                    header = stringResource(R.string.led),
                    footer = stringResource(R.string.led_section_footer),
                ) {
                    LedRow(uiState = uiState, onLedToggle = onLedToggle)
                }
            }
            item {
                SettingsSection(
                    header = stringResource(R.string.button),
                    footer = stringResource(R.string.button_section_footer),
                ) {
                    ButtonRow(uiState = uiState)
                }
            }

            // Both dashboards are gated on *any* field of that kind having arrived, not all of them —
            // a Blinky-style device never sets them, so its sections stay hidden (plan §6.2, matching
            // how iOS gates on hasEnvironmentData/hasMotionData).
            if (uiState.hasEnvironmentData) {
                item { EnvironmentSection(uiState) }
            }
            if (uiState.hasMotionData) {
                item { MotionSection(uiState) }
            }
        }
    }
}

@Composable
private fun EnvironmentSection(uiState: ThingyDetailUiState) {
    SettingsSection(
        header = stringResource(R.string.environment),
        footer = stringResource(R.string.environment_section_footer),
    ) {
        SensorRow(
            icon = R.drawable.ic_temperature,
            label = stringResource(R.string.temperature),
            value = SensorFormat.temperature(uiState.temperature),
        )
        SensorRow(
            icon = R.drawable.ic_humidity,
            label = stringResource(R.string.humidity),
            value = SensorFormat.humidity(uiState.humidity),
        )
        SensorRow(
            icon = R.drawable.ic_pressure,
            label = stringResource(R.string.pressure),
            value = SensorFormat.pressure(uiState.pressure),
        )
        SensorRow(
            icon = R.drawable.ic_air_quality,
            label = stringResource(R.string.air_quality),
            value = SensorFormat.airQuality(uiState.eco2, uiState.tvoc),
        )
    }
}

@Composable
private fun MotionSection(uiState: ThingyDetailUiState) {
    SettingsSection(
        header = stringResource(R.string.motion),
        footer = stringResource(R.string.motion_section_footer),
    ) {
        SensorRow(
            icon = R.drawable.ic_orientation,
            label = stringResource(R.string.orientation),
            value = uiState.orientation?.let { stringResource(it.labelRes) },
        )
        SensorRow(
            icon = R.drawable.ic_steps,
            label = stringResource(R.string.steps),
            value = SensorFormat.steps(uiState.stepCount),
        )
        SensorRow(
            icon = R.drawable.ic_heading,
            label = stringResource(R.string.heading),
            value = SensorFormat.heading(uiState.heading),
        )
        SensorRow(
            icon = R.drawable.ic_tap,
            label = stringResource(R.string.last_tap),
            value = SensorFormat.lastTap(uiState.lastTapDirection, uiState.lastTapCount),
        )
    }
}

@Composable
private fun LedRow(uiState: ThingyDetailUiState, onLedToggle: (Boolean) -> Unit) {
    val disconnected = uiState.connectionState == ConnectionState.DISCONNECTED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lightbulb),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(text = stringResource(ledStateRes(uiState)), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = uiState.ledIsOn,
            onCheckedChange = onLedToggle,
            enabled = uiState.connectionState == ConnectionState.CONNECTED && uiState.ledSupported,
            // iOS tints the toggle nordicRed while disconnected.
            colors = if (disconnected) {
                SwitchDefaults.colors(
                    checkedThumbColor = NordicColors.nordicRed,
                    checkedTrackColor = NordicColors.nordicRed.copy(alpha = 0.5f),
                )
            } else {
                SwitchDefaults.colors()
            },
        )
    }
}

@Composable
private fun ButtonRow(uiState: ThingyDetailUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_radio_button),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(buttonStateRes(uiState)),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// Derived state, ported from the iOS view's ledStateText/buttonStateText. Kept as pure functions
// returning resource ids so they're unit-testable without a Compose or Android context.
@StringRes
internal fun ledStateRes(uiState: ThingyDetailUiState): Int = when (uiState.connectionState) {
    ConnectionState.CONNECTING -> R.string.scanning
    ConnectionState.DISCONNECTED -> R.string.disconnected
    ConnectionState.CONNECTED -> if (uiState.ledIsOn) R.string.on else R.string.off
}

@StringRes
internal fun buttonStateRes(uiState: ThingyDetailUiState): Int = when (uiState.connectionState) {
    ConnectionState.CONNECTING -> R.string.scanning
    ConnectionState.DISCONNECTED -> R.string.disconnected
    ConnectionState.CONNECTED -> if (uiState.buttonPressed) R.string.pressed else R.string.released
}

@Preview(showBackground = true)
@Composable
private fun ThingyDetailScreenConnectedPreview() {
    ThingyTheme {
        ThingyDetailScreen(
            uiState = ThingyDetailUiState(
                name = "Thingy52 Mock",
                connectionState = ConnectionState.CONNECTED,
                ledSupported = true,
                buttonSupported = true,
                ledIsOn = true,
                buttonPressed = true,
            ),
            onLedToggle = {},
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ThingyDetailScreenWithDashboardsPreview() {
    ThingyTheme {
        ThingyDetailScreen(
            uiState = ThingyDetailUiState(
                name = "Thingy52 Mock",
                connectionState = ConnectionState.CONNECTED,
                ledSupported = true,
                buttonSupported = true,
                temperature = 22.5,
                humidity = 47,
                pressure = 1013.25,
                eco2 = 450,
                tvoc = 1200,
                orientation = ThingyOrientation.PORTRAIT,
                stepCount = 1234,
                heading = 271.5,
                lastTapDirection = TapDirection.Y_NEGATIVE,
                lastTapCount = 3,
            ),
            onLedToggle = {},
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThingyDetailScreenDisconnectedPreview() {
    ThingyTheme {
        ThingyDetailScreen(
            uiState = ThingyDetailUiState(
                name = "Thingy52 Mock",
                connectionState = ConnectionState.DISCONNECTED,
                ledSupported = true,
                ledIsOn = true,
            ),
            onLedToggle = {},
            onConnect = {},
            onDisconnect = {},
        )
    }
}
