package com.armstrongmobile.nrfthingy52android.ble.fake

import com.armstrongmobile.nrfthingy52android.MainDispatcherRule
import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import com.armstrongmobile.nrfthingy52android.ui.detail.ConnectionState
import com.armstrongmobile.nrfthingy52android.ui.detail.ThingyConnectionViewModel
import com.armstrongmobile.nrfthingy52android.ui.scanner.ScannerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// End-to-end pipeline tests against the fake Thingy:52 — the port of the iOS
// ThingyIntegrationTests suite (plan §9.2), which runs against CoreBluetoothMock on the simulator.
//
// These are plain JVM unit tests, not instrumented ones. The fake transport and both ViewModels are
// pure Kotlin, so nothing here needs an emulator, Bluetooth hardware, or even Robolectric — which is
// what plan §9.2 leaves open ("whichever the team prefers") and what Phase 8's DoD asks for.
//
// The iOS suite polls with waitUntil(...) because CoreBluetoothMock delivers asynchronously. Here the
// fake emits synchronously and MainDispatcherRule installs an unconfined dispatcher, so an event is
// observable on the ViewModel the moment it is simulated — no waiting, and no flakiness.
class ThingyPipelineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mirrors the iOS discoverThingy() helper.
    private fun discoverThingy(): Pair<ScannerViewModel, FakeBleScanner> {
        val scanner = FakeBleScanner()
        val viewModel = ScannerViewModel(scanner, unknownDeviceName = "Unknown Device")
        viewModel.startScan()
        return viewModel to scanner
    }

    // Mirrors the iOS connectToThingy() helper: discover, select, connect, land in CONNECTED.
    private fun connectToThingy(): Pair<FakeThingyController, ThingyConnectionViewModel> {
        val controller = FakeThingyController(advertisedName = ThingyMocks.MOCK_NAME, autoConnect = true)
        val viewModel = ThingyConnectionViewModel(controller, unknownDeviceName = "Unknown Device")
        viewModel.connect()
        assertEquals(ConnectionState.CONNECTED, viewModel.uiState.value.connectionState)
        return controller to viewModel
    }

    // Item 1: discovery via the scan filter, with the advertised name.
    @Test
    fun discoveryFindsAdvertisingThingy() {
        val (viewModel, scanner) = discoverThingy()

        val row = viewModel.uiState.value.discovered.single()
        assertEquals(ThingyMocks.MOCK_NAME, row.name)
        assertEquals(FakeBleScanner.MOCK_ADDRESS, row.address)
        assertTrue(viewModel.uiState.value.isScanning)

        viewModel.stopScan()
        assertFalse(scanner.isScanning.value)
    }

    // Item 2: connecting runs the discover/notify/read pipeline and publishes support flags.
    @Test
    fun connectDiscoversLedAndButton() {
        val (_, viewModel) = connectToThingy()

        assertTrue(viewModel.uiState.value.ledSupported)
        assertTrue(viewModel.uiState.value.buttonSupported)

        viewModel.disconnect()
    }

    // Item 3: the LED write reaches the simulated firmware and is confirmed by the read-back.
    // `controller.ledIsOn` is the firmware-held state, the analogue of iOS's ThingyMocks.ledIsOn.
    @Test
    fun ledToggleWritesAndReadsBack() {
        val (controller, viewModel) = connectToThingy()

        viewModel.setLed(on = true)
        assertTrue(controller.ledIsOn)
        assertTrue(viewModel.uiState.value.ledIsOn)

        viewModel.setLed(on = false)
        assertFalse(controller.ledIsOn)
        assertFalse(viewModel.uiState.value.ledIsOn)

        viewModel.disconnect()
    }

    // Item 4: button notifications propagate into UI state.
    @Test
    fun buttonPressAndReleaseNotify() {
        val (controller, viewModel) = connectToThingy()

        controller.pressButton()
        assertTrue(viewModel.uiState.value.buttonPressed)

        controller.releaseButton()
        assertFalse(viewModel.uiState.value.buttonPressed)

        viewModel.disconnect()
    }

    // Item 5: on-demand disconnect (navigating away) reaches the disconnected state.
    @Test
    fun disconnectOnDemand() {
        val (_, viewModel) = connectToThingy()

        viewModel.disconnect()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }

    // Item 6: Bluetooth powering off surfaces the same disconnected state.
    @Test
    fun powerOffDisconnects() {
        val (controller, viewModel) = connectToThingy()

        controller.powerOff()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }

    @Test
    fun environmentReadingsStreamToConnection() {
        val (controller, viewModel) = connectToThingy()
        assertFalse(viewModel.uiState.value.hasEnvironmentData)

        controller.simulateEnvironment(
            temperature = 23.5,
            humidity = 48,
            pressure = 1008.75,
            eco2 = 520,
            tvoc = 34,
        )

        val state = viewModel.uiState.value
        assertTrue(state.hasEnvironmentData)
        assertEquals(23.5, state.temperature!!, 0.001)
        assertEquals(48, state.humidity)
        assertEquals(1008.75, state.pressure!!, 0.001)
        assertEquals(520, state.eco2)
        assertEquals(34, state.tvoc)

        viewModel.disconnect()
    }

    @Test
    fun motionReadingsStreamToConnection() {
        val (controller, viewModel) = connectToThingy()
        assertFalse(viewModel.uiState.value.hasMotionData)

        controller.simulateOrientation(ThingyOrientation.LANDSCAPE)
        controller.simulateTap(direction = TapDirection.Z_POSITIVE, count = 2)
        controller.simulateStepCount(steps = 42, durationSeconds = 12.5)
        controller.simulateHeading(degrees = 90.0)

        val state = viewModel.uiState.value
        assertTrue(state.hasMotionData)
        assertEquals(ThingyOrientation.LANDSCAPE, state.orientation)
        assertEquals(TapDirection.Z_POSITIVE, state.lastTapDirection)
        assertEquals(2, state.lastTapCount)
        assertEquals(42, state.stepCount)
        assertEquals(12.5, state.stepDurationSeconds!!, 0.01)
        assertEquals(90.0, state.heading!!, 0.001)

        viewModel.disconnect()
    }

    // Item 7 variant: a peripheral-initiated disconnection surfaces the disconnected state too.
    @Test
    fun peripheralInitiatedDisconnect() {
        val (controller, viewModel) = connectToThingy()

        controller.disconnectThingy()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }
}
