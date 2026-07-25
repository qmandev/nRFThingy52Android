package com.armstrongmobile.nrfthingy52android.ble.fake

import com.armstrongmobile.nrfthingy52android.ble.BleScanner
import com.armstrongmobile.nrfthingy52android.ble.ThingyScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Scanner double that "discovers" the one canned mock Thingy while scanning, standing in for the
// advertising CoreBluetoothMock peripheral on iOS (plan §9.1). Re-emits on an interval so RSSI
// changes and the row-refresh throttle can be exercised.
class FakeBleScanner(
    private val device: ThingyScanResult = ThingyScanResult(
        address = MOCK_ADDRESS,
        name = ThingyMocks.MOCK_NAME,
        rssi = -45,
    ),
) : BleScanner {

    private val _scanResults = MutableSharedFlow<ThingyScanResult>(extraBufferCapacity = 32)
    override val scanResults: Flow<ThingyScanResult> = _scanResults.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startScan() {
        _isScanning.value = true
        // Report the device immediately so a collector sees it without waiting on a timer.
        _scanResults.tryEmit(device)
    }

    override fun stopScan() {
        _isScanning.value = false
    }

    // Test/demo control: report another sighting, optionally at a different signal strength.
    fun simulateDiscovery(rssi: Int = device.rssi) {
        _scanResults.tryEmit(device.copy(rssi = rssi))
    }

    companion object {
        const val MOCK_ADDRESS = "AA:BB:CC:DD:EE:FF"
    }
}
