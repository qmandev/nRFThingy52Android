package com.armstrongmobile.nrfthingy52android.ble

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Stands in when an address can't be resolved to a real peripheral — a malformed or stale navigation
// argument, or Bluetooth being off when the detail screen opens. connect() reports Disconnected
// immediately, so the UI shows the disconnected state instead of hanging on "Scanning..." forever
// (the Android equivalent of iOS's didFailToConnect path; plan §4 point 11).
class UnavailableThingyController(override val advertisedName: String? = null) : ThingyController {

    private val _events = MutableSharedFlow<ThingyGattEvent>(extraBufferCapacity = 4)
    override val events: SharedFlow<ThingyGattEvent> = _events.asSharedFlow()

    override val isConnected: Boolean = false

    override fun connect() {
        _events.tryEmit(ThingyGattEvent.Disconnected)
    }

    override fun disconnect() = Unit

    override fun turnOnLed() = Unit

    override fun turnOffLed() = Unit
}
