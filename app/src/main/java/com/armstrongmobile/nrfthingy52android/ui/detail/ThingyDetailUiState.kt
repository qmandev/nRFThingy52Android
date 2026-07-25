package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation

enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }

// Everything the detail screen renders, mirroring the observable properties of the iOS
// ThingyConnection. Sensor fields are null until the first reading of that kind arrives.
data class ThingyDetailUiState(
    val name: String = "",
    val connectionState: ConnectionState = ConnectionState.CONNECTING,
    val ledSupported: Boolean = false,
    val buttonSupported: Boolean = false,
    val ledIsOn: Boolean = false,
    val buttonPressed: Boolean = false,
    // Environment
    val temperature: Double? = null,
    val humidity: Int? = null,
    val pressure: Double? = null,
    val eco2: Int? = null,
    val tvoc: Int? = null,
    // Motion
    val orientation: ThingyOrientation? = null,
    val stepCount: Int? = null,
    val stepDurationSeconds: Double? = null,
    val heading: Double? = null,
    val lastTapDirection: TapDirection? = null,
    val lastTapCount: Int? = null,
) {
    // Gate the dashboard sections on *any* field having arrived, not all of them — a Blinky-style
    // device never sets these, so its dashboards stay hidden (plan §6.2, matching iOS exactly).
    val hasEnvironmentData: Boolean
        get() = temperature != null || humidity != null || pressure != null || eco2 != null

    val hasMotionData: Boolean
        get() = orientation != null || stepCount != null || heading != null || lastTapDirection != null
}
