package com.armstrongmobile.nrfthingy52android.ble.fake

import com.armstrongmobile.nrfthingy52android.ble.ThingyController
import com.armstrongmobile.nrfthingy52android.ble.ThingyGattEvent
import com.armstrongmobile.nrfthingy52android.domain.EnvironmentReading
import com.armstrongmobile.nrfthingy52android.domain.MotionReading
import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyEnvironment
import com.armstrongmobile.nrfthingy52android.domain.ThingyMotion
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// In-memory Thingy:52, replacing the iOS app's CoreBluetoothMock + MockThingy52 simulated firmware
// (plan §9.1). Android has no drop-in GATT-layer mock library, so this hand-rolled double is what
// lets the detail screen, dashboards, and their tests run with no hardware.
//
// The `simulate*` / `pressButton` / `releaseButton` / `powerOff` / `disconnectThingy` controls and
// the `ledIsOn` property intentionally keep the names ThingyMocks uses on iOS, so tests cross-
// reference 1:1 against the iOS suite.
//
// Sensor simulation encodes the value and parses it straight back, so every simulated reading
// exercises the real Phase 2 wire-format codecs — the same thing CoreBluetoothMock achieves by
// pushing raw Data through the parse path.
class FakeThingyController(
    override var advertisedName: String? = "Mock Thingy",
    // When true, connect()/disconnect() immediately emit the matching lifecycle event, so the fake
    // behaves like a real device for integration/demo use. Left false for ViewModel unit tests,
    // which drive the lifecycle explicitly — matching the dumb `MockThingy` recorder the iOS
    // ThingyConnectionTests use.
    private val autoConnect: Boolean = false,
    private val ledSupported: Boolean = true,
    private val buttonSupported: Boolean = true,
) : ThingyController {

    private val _events = MutableSharedFlow<ThingyGattEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<ThingyGattEvent> = _events.asSharedFlow()

    override var isConnected: Boolean = false
        private set

    // Call counters, mirroring the iOS MockThingy recorder.
    var connectCalls: Int = 0
        private set
    var disconnectCalls: Int = 0
        private set
    var turnOnCalls: Int = 0
        private set
    var turnOffCalls: Int = 0
        private set

    // LED state as held by the simulated firmware (iOS: ThingyMocks.ledIsOn).
    var ledIsOn: Boolean = false
        private set

    var buttonPressed: Boolean = false
        private set

    override fun connect() {
        connectCalls++
        if (autoConnect) simulateConnected()
    }

    override fun disconnect() {
        disconnectCalls++
        if (autoConnect) simulateDisconnection()
    }

    // Writes are confirmed by a read-back, exactly as the real transport does: the firmware records
    // the new value and reports the authoritative state, which may correct an optimistic UI update
    // (plan §4 point 10).
    override fun turnOnLed() {
        turnOnCalls++
        ledIsOn = true
        emit(ThingyGattEvent.LedStateChanged(isOn = true))
    }

    override fun turnOffLed() {
        turnOffCalls++
        ledIsOn = false
        emit(ThingyGattEvent.LedStateChanged(isOn = false))
    }

    // MARK: Lifecycle controls

    fun simulateConnected(
        ledSupported: Boolean = this.ledSupported,
        buttonSupported: Boolean = this.buttonSupported,
    ) {
        isConnected = true
        emit(ThingyGattEvent.Connected(ledSupported = ledSupported, buttonSupported = buttonSupported))
    }

    fun simulateDisconnection() {
        isConnected = false
        emit(ThingyGattEvent.Disconnected)
    }

    // Bluetooth turned off mid-session resolves to the same disconnected outcome (plan §4 point 11).
    fun powerOff() = simulateDisconnection()

    fun disconnectThingy() = simulateDisconnection()

    // MARK: Button controls

    fun pressButton() {
        buttonPressed = true
        emit(ThingyGattEvent.ButtonStateChanged(isPressed = true))
    }

    fun releaseButton() {
        buttonPressed = false
        emit(ThingyGattEvent.ButtonStateChanged(isPressed = false))
    }

    // MARK: Environment simulation

    fun simulateEnvironment(
        temperature: Double,
        humidity: Int,
        pressure: Double,
        eco2: Int,
        tvoc: Int,
    ) {
        emitEnvironment(ThingyEnvironment.encodeTemperature(temperature), ThingyEnvironment::parseTemperature)
        emitEnvironment(ThingyEnvironment.encodeHumidity(humidity), ThingyEnvironment::parseHumidity)
        emitEnvironment(ThingyEnvironment.encodePressure(pressure), ThingyEnvironment::parsePressure)
        emitEnvironment(ThingyEnvironment.encodeAirQuality(eco2, tvoc), ThingyEnvironment::parseAirQuality)
    }

    // MARK: Motion simulation

    fun simulateOrientation(orientation: ThingyOrientation) {
        emitMotion(ThingyMotion.encodeOrientation(orientation), ThingyMotion::parseOrientation)
    }

    fun simulateTap(direction: TapDirection, count: Int = 1) {
        emitMotion(ThingyMotion.encodeTap(direction, count), ThingyMotion::parseTap)
    }

    fun simulateStepCount(steps: Int, durationSeconds: Double) {
        emitMotion(ThingyMotion.encodeStepCount(steps, durationSeconds), ThingyMotion::parseStepCount)
    }

    fun simulateHeading(degrees: Double) {
        emitMotion(ThingyMotion.encodeHeading(degrees), ThingyMotion::parseHeading)
    }

    private fun emitEnvironment(data: ByteArray, parse: (ByteArray) -> EnvironmentReading?) {
        parse(data)?.let { emit(ThingyGattEvent.EnvironmentUpdate(it)) }
    }

    private fun emitMotion(data: ByteArray, parse: (ByteArray) -> MotionReading?) {
        parse(data)?.let { emit(ThingyGattEvent.MotionUpdate(it)) }
    }

    // tryEmit is safe from any thread; the buffer above absorbs bursts without suspending.
    private fun emit(event: ThingyGattEvent) {
        _events.tryEmit(event)
    }
}
