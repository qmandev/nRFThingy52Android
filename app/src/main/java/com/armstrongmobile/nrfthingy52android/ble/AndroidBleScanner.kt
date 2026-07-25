package com.armstrongmobile.nrfthingy52android.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.armstrongmobile.nrfthingy52android.domain.ThingyUserInterface
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Real scanning over BluetoothLeScanner, filtered to the Thingy UI service so only Thingy:52s are
// reported — the equivalent of iOS's scanForPeripherals(withServices: [thingyUIService]).
//
// iOS additionally passes CBCentralManagerScanOptionAllowDuplicatesKey: true to receive repeated
// advertisements for live RSSI. SCAN_MODE_LOW_LATENCY plus the default (non-batched) callback type is
// the Android equivalent — every advertisement is reported, and dedupe/throttle by address happens in
// ScannerViewModel (plan §4 point 8).
//
// ScanCallback fires on a Binder thread; results are only ever tryEmit()-ed here, never applied to
// state (plan §4.1). Callers must hold the scan permissions (see BlePermissions) before starting.
@SuppressLint("MissingPermission")
class AndroidBleScanner(context: Context) : BleScanner {

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

    // Dropping the oldest sighting under load is correct here: advertisements are a continuous
    // stream and the newest RSSI is the one worth showing.
    private val _scanResults = MutableSharedFlow<ThingyScanResult>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val scanResults: Flow<ThingyScanResult> = _scanResults.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            _scanResults.tryEmit(result.toThingyScanResult())
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { _scanResults.tryEmit(it.toThingyScanResult()) }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    override fun startScan() {
        if (_isScanning.value) return
        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner ?: return

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ThingyUserInterface.serviceUuid))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Throws if the adapter turned off between the null-check and here, or if a permission was
        // revoked mid-session; either way scanning simply hasn't started.
        runCatching { scanner.startScan(filters, settings, callback) }
            .onSuccess { _isScanning.value = true }
    }

    override fun stopScan() {
        if (!_isScanning.value) return
        _isScanning.value = false
        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner ?: return
        runCatching { scanner.stopScan(callback) }
    }

    // The advertised local name, falling back to the device name; either may be absent.
    private fun ScanResult.toThingyScanResult() = ThingyScanResult(
        address = device.address,
        name = scanRecord?.deviceName ?: runCatching { device.name }.getOrNull(),
        rssi = rssi,
    )
}
