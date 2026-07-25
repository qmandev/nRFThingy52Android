package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.R
import org.junit.Assert.assertEquals
import org.junit.Test

// Covers the derived row text ported from the iOS view's ledStateText/buttonStateText. R.string ids
// are plain int constants, so this needs no Android context.
class DetailStateTextTest {

    @Test
    fun ledTextIsScanningWhileConnecting() {
        assertEquals(
            R.string.scanning,
            ledStateRes(ThingyDetailUiState(connectionState = ConnectionState.CONNECTING, ledIsOn = true)),
        )
    }

    // Disconnected wins over the last known LED value, exactly as iOS switches on state first.
    @Test
    fun ledTextIsDisconnectedWhileDisconnected() {
        assertEquals(
            R.string.disconnected,
            ledStateRes(ThingyDetailUiState(connectionState = ConnectionState.DISCONNECTED, ledIsOn = true)),
        )
    }

    @Test
    fun ledTextReflectsStateWhileConnected() {
        assertEquals(
            R.string.on,
            ledStateRes(ThingyDetailUiState(connectionState = ConnectionState.CONNECTED, ledIsOn = true)),
        )
        assertEquals(
            R.string.off,
            ledStateRes(ThingyDetailUiState(connectionState = ConnectionState.CONNECTED, ledIsOn = false)),
        )
    }

    @Test
    fun buttonTextIsScanningWhileConnecting() {
        assertEquals(
            R.string.scanning,
            buttonStateRes(ThingyDetailUiState(connectionState = ConnectionState.CONNECTING, buttonPressed = true)),
        )
    }

    @Test
    fun buttonTextIsDisconnectedWhileDisconnected() {
        assertEquals(
            R.string.disconnected,
            buttonStateRes(ThingyDetailUiState(connectionState = ConnectionState.DISCONNECTED, buttonPressed = true)),
        )
    }

    @Test
    fun buttonTextReflectsStateWhileConnected() {
        assertEquals(
            R.string.pressed,
            buttonStateRes(ThingyDetailUiState(connectionState = ConnectionState.CONNECTED, buttonPressed = true)),
        )
        assertEquals(
            R.string.released,
            buttonStateRes(ThingyDetailUiState(connectionState = ConnectionState.CONNECTED, buttonPressed = false)),
        )
    }
}
