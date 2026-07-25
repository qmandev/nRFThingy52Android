package com.armstrongmobile.nrfthingy52android.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import com.armstrongmobile.nrfthingy52android.domain.EnvironmentReading
import com.armstrongmobile.nrfthingy52android.domain.MotionReading
import com.armstrongmobile.nrfthingy52android.domain.ThingyEnvironment
import com.armstrongmobile.nrfthingy52android.domain.ThingyMotion
import com.armstrongmobile.nrfthingy52android.domain.ThingyUserInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ConnectionPriorityRequest
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.UUID

// The real BLE state machine for one physical Thingy:52 — the port of the iOS ThingyPeripheral,
// driving the same connect → discover → enable-notifications → read pipeline (plan §4 points 9–11).
//
// Built on Nordic's BleManager (plan §10.5) rather than raw BluetoothGatt, which supplies the three
// things the raw API makes you hand-roll and that are the usual sources of flaky Android BLE:
//   1. A GATT operation queue — raw BluetoothGatt drops overlapping operations (§4 point 7). Every
//      request below is enqueued and drained one at a time by the library.
//   2. Notification enabling — enableNotifications() performs both the setCharacteristicNotification
//      call *and* the CCCD descriptor write. Missing the second step is the classic reason Android
//      notifications silently never arrive (§4 point 9); iOS's setNotifyValue does both internally.
//   3. close(), which Android requires to release the GATT client on disconnect (§4 point 11) and
//      which CoreBluetooth has no equivalent of.
//
// BleManager is wrapped rather than subclassed: its isConnected()/disconnect() members collide with
// the ThingyController interface (disconnect() is final and returns a request builder), and its
// request builders are protected, so ThingyBleManager below exposes narrow wrappers over them.
//
// THREADING: library callbacks arrive off the main thread. Nothing here touches Compose state — each
// callback becomes a ThingyGattEvent via tryEmit(), which is safe from any thread. The ViewModel's
// single collection point is where state is mutated (plan §4.1).
//
// MTU: not negotiated. Every characteristic here is ≤ 8 bytes (step counter is the largest: two
// uint32s), so the default 23-byte ATT MTU suffices (plan §4.2). If the deferred quaternion/Euler/
// raw-accelerometer characteristics are ever added, request a larger MTU at that point.
class ThingyGatt(
    context: Context,
    private val device: BluetoothDevice,
) : ThingyController {

    private val _events = MutableSharedFlow<ThingyGattEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<ThingyGattEvent> = _events.asSharedFlow()

    private val manager = ThingyBleManager(context) { _events.tryEmit(it) }

    // Reading device.name needs BLUETOOTH_CONNECT. The UI gates connection on the permission (see
    // BlePermissions) and runCatching absorbs the SecurityException if it was revoked mid-session —
    // lint can't see through runCatching, hence the suppression.
    @get:SuppressLint("MissingPermission")
    override val advertisedName: String?
        get() = runCatching { device.name }.getOrNull()

    override val isConnected: Boolean
        get() = manager.isConnected

    override fun connect() {
        if (manager.isConnected) return
        manager.connectTo(device)
    }

    override fun disconnect() = manager.disconnectFromDevice()

    override fun turnOnLed() = manager.writeLed(on = true)

    override fun turnOffLed() = manager.writeLed(on = false)

    // Releases the underlying GATT client. Android requires this; CoreBluetooth handles it
    // implicitly, so there is no iOS call to port (plan §4 point 11).
    fun close() = manager.close()
}

// Permissions are checked by the UI layer before connecting (see BlePermissions); the library calls
// require BLUETOOTH_CONNECT, hence the suppression.
@SuppressLint("MissingPermission")
private class ThingyBleManager(
    context: Context,
    private val emit: (ThingyGattEvent) -> Unit,
) : BleManager(context) {

    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var buttonCharacteristic: BluetoothGattCharacteristic? = null
    private val environmentCharacteristics =
        mutableMapOf<BluetoothGattCharacteristic, (ByteArray) -> EnvironmentReading?>()
    private val motionCharacteristics =
        mutableMapOf<BluetoothGattCharacteristic, (ByteArray) -> MotionReading?>()

    init {
        setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) = Unit
            override fun onDeviceConnected(device: BluetoothDevice) = Unit

            // All three iOS disconnect paths — on-demand, failed attempt, and link loss / adapter
            // off — resolve to the same disconnected outcome (plan §4 point 11).
            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                emit(ThingyGattEvent.Disconnected)
            }

            override fun onDeviceReady(device: BluetoothDevice) = Unit
            override fun onDeviceDisconnecting(device: BluetoothDevice) = Unit

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                emit(ThingyGattEvent.Disconnected)
            }
        })
    }

    fun connectTo(device: BluetoothDevice) {
        connect(device)
            .useAutoConnect(false)
            .retry(CONNECT_RETRIES, CONNECT_RETRY_DELAY_MS)
            .timeout(CONNECT_TIMEOUT_MS)
            .enqueue()
    }

    fun disconnectFromDevice() {
        disconnect().enqueue()
    }

    // Write with response, then read back the authoritative value — the read-back may correct an
    // optimistic UI update if the device rejected or altered it (plan §4 point 10, matching iOS).
    fun writeLed(on: Boolean) {
        val characteristic = ledCharacteristic ?: return
        writeCharacteristic(
            characteristic,
            ThingyUserInterface.encodeLed(on),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        ).done { readLed() }.enqueue()
    }

    // Discovery: locate the characteristics this app uses. Returning false makes the library abort
    // the connection, matching iOS's "device supports neither LED nor button, disconnect" rule —
    // Environment/Motion alone are not enough to show the detail screen.
    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        gatt.getService(ThingyUserInterface.serviceUuid)?.let { service ->
            ledCharacteristic = service.getCharacteristic(ThingyUserInterface.ledCharacteristicUuid)
            buttonCharacteristic = service.getCharacteristic(ThingyUserInterface.buttonCharacteristicUuid)
        }

        gatt.getService(ThingyEnvironment.serviceUuid)?.let { service ->
            service.register(ThingyEnvironment.temperatureCharacteristicUuid, environmentCharacteristics, ThingyEnvironment::parseTemperature)
            service.register(ThingyEnvironment.pressureCharacteristicUuid, environmentCharacteristics, ThingyEnvironment::parsePressure)
            service.register(ThingyEnvironment.humidityCharacteristicUuid, environmentCharacteristics, ThingyEnvironment::parseHumidity)
            service.register(ThingyEnvironment.airQualityCharacteristicUuid, environmentCharacteristics, ThingyEnvironment::parseAirQuality)
        }

        gatt.getService(ThingyMotion.serviceUuid)?.let { service ->
            service.register(ThingyMotion.tapCharacteristicUuid, motionCharacteristics, ThingyMotion::parseTap)
            service.register(ThingyMotion.orientationCharacteristicUuid, motionCharacteristics, ThingyMotion::parseOrientation)
            service.register(ThingyMotion.stepCounterCharacteristicUuid, motionCharacteristics, ThingyMotion::parseStepCount)
            service.register(ThingyMotion.headingCharacteristicUuid, motionCharacteristics, ThingyMotion::parseHeading)
        }

        return ledCharacteristic != null || buttonCharacteristic != null
    }

    // Runs after discovery succeeds; requests are queued and drained in order by the library.
    override fun initialize() {
        requestConnectionPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_BALANCED).enqueue()

        buttonCharacteristic?.let { characteristic ->
            setNotificationCallback(characteristic).with { _, data ->
                data.value?.let { bytes ->
                    ThingyUserInterface.parseButton(bytes)?.let { emit(ThingyGattEvent.ButtonStateChanged(it)) }
                }
            }
            enableNotifications(characteristic).enqueue()
            // Read the initial state so the UI isn't blank until the first press.
            readCharacteristic(characteristic).with { _, data ->
                data.value?.let { bytes ->
                    ThingyUserInterface.parseButton(bytes)?.let { emit(ThingyGattEvent.ButtonStateChanged(it)) }
                }
            }.enqueue()
        }

        if (ledCharacteristic != null) readLed()

        environmentCharacteristics.forEach { (characteristic, parse) ->
            setNotificationCallback(characteristic).with { _, data ->
                data.value?.let { bytes -> parse(bytes)?.let { emit(ThingyGattEvent.EnvironmentUpdate(it)) } }
            }
            enableNotifications(characteristic).enqueue()
        }

        motionCharacteristics.forEach { (characteristic, parse) ->
            setNotificationCallback(characteristic).with { _, data ->
                data.value?.let { bytes -> parse(bytes)?.let { emit(ThingyGattEvent.MotionUpdate(it)) } }
            }
            enableNotifications(characteristic).enqueue()
        }

        emit(
            ThingyGattEvent.Connected(
                ledSupported = ledCharacteristic != null,
                buttonSupported = buttonCharacteristic != null,
            )
        )
    }

    override fun onServicesInvalidated() {
        ledCharacteristic = null
        buttonCharacteristic = null
        environmentCharacteristics.clear()
        motionCharacteristics.clear()
    }

    private fun readLed() {
        val characteristic = ledCharacteristic ?: return
        readCharacteristic(characteristic).with { _, data ->
            data.value?.let { bytes ->
                ThingyUserInterface.parseLed(bytes)?.let { emit(ThingyGattEvent.LedStateChanged(it)) }
            }
        }.enqueue()
    }

    private fun <T> BluetoothGattService.register(
        uuid: UUID,
        into: MutableMap<BluetoothGattCharacteristic, (ByteArray) -> T?>,
        parse: (ByteArray) -> T?,
    ) {
        getCharacteristic(uuid)?.let { into[it] = parse }
    }

    private companion object {
        const val CONNECT_RETRIES = 3
        const val CONNECT_RETRY_DELAY_MS = 100
        const val CONNECT_TIMEOUT_MS = 15_000L
    }
}
