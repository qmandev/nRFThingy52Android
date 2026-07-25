package com.armstrongmobile.nrfthingy52android.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.armstrongmobile.nrfthingy52android.ble.ThingyController
import com.armstrongmobile.nrfthingy52android.ble.ThingyGatt
import com.armstrongmobile.nrfthingy52android.ble.fake.ThingyMocks

// Resolves a MAC address to the controller for that peripheral, and caches it so the scanner and the
// detail screen share one connection per device.
//
// This exists because Navigation Compose routes carry only primitive arguments: the detail
// ViewModel receives a `deviceAddress: String` and looks the peripheral up here, rather than being
// handed an object reference the way iOS's ThingyConnection(peripheral:) is (plan §3, §6.1).
interface ThingyRepository {
    fun controllerFor(address: String): ThingyController?

    // Releases the GATT client for one device. Android requires close() on disconnect, which
    // CoreBluetooth handles implicitly (plan §4 point 11).
    fun release(address: String)
}

class RealThingyRepository(private val context: Context) : ThingyRepository {

    private val controllers = mutableMapOf<String, ThingyGatt>()

    override fun controllerFor(address: String): ThingyController? = synchronized(controllers) {
        controllers[address] ?: createController(address)?.also { controllers[address] = it }
    }

    override fun release(address: String) = synchronized(controllers) {
        controllers.remove(address)?.close() ?: Unit
    }

    private fun createController(address: String): ThingyGatt? {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
        // getRemoteDevice rejects a malformed address; a stale nav argument shouldn't crash the app.
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return null
        return ThingyGatt(context, device)
    }
}

// Mock-flavor repository: every address resolves to the one simulated Thingy, so the detail screen
// works no matter what the scanner reported.
class FakeThingyRepository : ThingyRepository {
    override fun controllerFor(address: String): ThingyController = ThingyMocks.controller

    override fun release(address: String) = Unit
}
