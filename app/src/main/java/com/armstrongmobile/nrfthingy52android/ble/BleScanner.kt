package com.armstrongmobile.nrfthingy52android.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// One advertisement sighting. Deliberately raw: the address is the dedupe key (Android's analogue of
// CBPeripheral.identifier) and `rssi` stays an Int here, so bucketing/dedupe/throttling live in
// ScannerViewModel (plan §4 point 8) rather than in the transport.
data class ThingyScanResult(
    val address: String,
    val name: String?,
    val rssi: Int,
)

// Scanning seam, mirroring how iOS's ScannerModel owns the CBCentralManager. Implemented by a real
// BluetoothLeScanner wrapper (Phase 4) and by FakeBleScanner (Phase 3).
//
// Results may be delivered from any thread (Android's ScanCallback fires on a Binder thread), so
// collectors choose their own dispatcher (plan §4.1).
interface BleScanner {
    val scanResults: Flow<ThingyScanResult>

    val isScanning: StateFlow<Boolean>

    fun startScan()

    fun stopScan()
}
