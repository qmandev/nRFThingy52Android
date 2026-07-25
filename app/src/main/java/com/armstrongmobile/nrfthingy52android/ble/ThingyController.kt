package com.armstrongmobile.nrfthingy52android.ble

import kotlinx.coroutines.flow.SharedFlow

// The BLE operations one Thingy connection exposes, and the seam that makes connection-state logic
// testable without a real BluetoothGatt — the direct analogue of the iOS `ThingyControlling`
// protocol. Implemented by the real Nordic-ble-backed transport (Phase 4) and by
// FakeThingyController (Phase 3).
//
// Implementations may deliver `events` from any thread; collectors are responsible for choosing the
// dispatcher they observe on (plan §4.1).
interface ThingyController {
    // The name advertised by the peripheral, or null if it advertised none.
    val advertisedName: String?

    val isConnected: Boolean

    // Connection lifecycle, LED/button state, and sensor readings for this peripheral.
    val events: SharedFlow<ThingyGattEvent>

    fun connect()

    fun disconnect()

    fun turnOnLed()

    fun turnOffLed()
}
