package com.armstrongmobile.nrfthingy52android.ble

import com.armstrongmobile.nrfthingy52android.domain.EnvironmentReading
import com.armstrongmobile.nrfthingy52android.domain.MotionReading

// One event from a Thingy connection — the plain-data equivalent of the iOS `ThingyDelegate`
// callbacks (thingyDidConnect / thingyDidDisconnect / buttonStateChanged / ledStateChanged /
// environmentDidUpdate / motionDidUpdate).
//
// Why events instead of iOS's delegate protocol (plan §3 names a `ThingyGattListener`): Android's
// BluetoothGattCallback fires on an internal Binder thread, so the transport must never call into
// Compose state directly (plan §4.1). Emitting immutable events onto a flow is thread-safe from any
// thread, and the owning ViewModel's single `viewModelScope` collection point is where state is
// mutated — that collection point is the concurrency boundary replacing iOS's @MainActor isolation.
sealed interface ThingyGattEvent {
    data class Connected(val ledSupported: Boolean, val buttonSupported: Boolean) : ThingyGattEvent
    data object Disconnected : ThingyGattEvent
    data class ButtonStateChanged(val isPressed: Boolean) : ThingyGattEvent
    data class LedStateChanged(val isOn: Boolean) : ThingyGattEvent
    data class EnvironmentUpdate(val reading: EnvironmentReading) : ThingyGattEvent
    data class MotionUpdate(val reading: MotionReading) : ThingyGattEvent
}
