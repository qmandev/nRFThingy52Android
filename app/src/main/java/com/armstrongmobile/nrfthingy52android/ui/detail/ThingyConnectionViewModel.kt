package com.armstrongmobile.nrfthingy52android.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armstrongmobile.nrfthingy52android.ble.ThingyController
import com.armstrongmobile.nrfthingy52android.ble.ThingyGattEvent
import com.armstrongmobile.nrfthingy52android.domain.EnvironmentReading
import com.armstrongmobile.nrfthingy52android.domain.MotionReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Connection state for one Thingy, for the Compose detail screen — the counterpart of the iOS
// ThingyConnection, which adopts ThingyDelegate and republishes callbacks as observable state.
//
// CONCURRENCY BOUNDARY (plan §4.1): the transport emits events from a Binder thread, and the
// collection below is the *only* place uiState is mutated. viewModelScope dispatches on
// Dispatchers.Main.immediate, so every state write lands on the main thread — this single collection
// point is what replaces iOS's compiler-enforced @MainActor isolation. There is no compiler check
// here, so do not mutate _uiState from anywhere else, and never call into Compose from the transport.
//
// Phase 3 injects the controller directly, mirroring iOS's `ThingyConnection(peripheral:)`. Phase 5/6
// adds the navigation-driven construction (deviceAddress from SavedStateHandle + a repository
// lookup), since Navigation Compose routes carry only primitive arguments (plan §3, §6.1).
class ThingyConnectionViewModel(
    private val controller: ThingyController,
    // Android has no global Bundle.main equivalent for a non-Composable call site, so the fallback
    // display name is passed in rather than resolved here (plan §3). Phase 8 wires the localized
    // R.string value at the construction site.
    private val unknownDeviceName: String = "Unknown Device",
) : ViewModel() {

    // Resolved on demand from the peripheral, like the iOS `ThingyConnection.name` computed property.
    val name: String
        get() = controller.advertisedName ?: unknownDeviceName

    private val _uiState = MutableStateFlow(
        ThingyDetailUiState(name = name, connectionState = ConnectionState.CONNECTING)
    )
    val uiState: StateFlow<ThingyDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            controller.events.collect(::onEvent)
        }
    }

    fun connect() {
        if (controller.isConnected) return
        _uiState.update { it.copy(connectionState = ConnectionState.CONNECTING) }
        controller.connect()
    }

    fun disconnect() {
        controller.disconnect()
    }

    // Sets the LED optimistically; the value is confirmed — or corrected — by the read-back that
    // arrives as a LedStateChanged event (plan §4 point 10).
    fun setLed(on: Boolean) {
        _uiState.update { it.copy(ledIsOn = on) }
        if (on) controller.turnOnLed() else controller.turnOffLed()
    }

    private fun onEvent(event: ThingyGattEvent) {
        when (event) {
            is ThingyGattEvent.Connected -> {
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.CONNECTED,
                        ledSupported = event.ledSupported,
                        buttonSupported = event.buttonSupported,
                    )
                }
                // Nothing to show for a device with neither LED nor button: disconnect.
                if (!event.ledSupported && !event.buttonSupported) controller.disconnect()
            }

            ThingyGattEvent.Disconnected ->
                _uiState.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }

            is ThingyGattEvent.ButtonStateChanged ->
                _uiState.update { it.copy(buttonPressed = event.isPressed) }

            is ThingyGattEvent.LedStateChanged ->
                _uiState.update { it.copy(ledIsOn = event.isOn) }

            is ThingyGattEvent.EnvironmentUpdate -> onEnvironmentReading(event.reading)

            is ThingyGattEvent.MotionUpdate -> onMotionReading(event.reading)
        }
    }

    private fun onEnvironmentReading(reading: EnvironmentReading) = _uiState.update { state ->
        when (reading) {
            is EnvironmentReading.Temperature -> state.copy(temperature = reading.celsius)
            is EnvironmentReading.Humidity -> state.copy(humidity = reading.percent)
            is EnvironmentReading.Pressure -> state.copy(pressure = reading.hPa)
            is EnvironmentReading.AirQuality -> state.copy(eco2 = reading.eco2, tvoc = reading.tvoc)
        }
    }

    private fun onMotionReading(reading: MotionReading) = _uiState.update { state ->
        when (reading) {
            is MotionReading.Tap -> state.copy(
                lastTapDirection = reading.direction,
                lastTapCount = reading.count,
            )
            is MotionReading.Orientation -> state.copy(orientation = reading.value)
            is MotionReading.StepCount -> state.copy(
                stepCount = reading.steps,
                stepDurationSeconds = reading.durationSeconds,
            )
            is MotionReading.Heading -> state.copy(heading = reading.degrees)
        }
    }
}
