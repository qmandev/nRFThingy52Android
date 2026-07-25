package com.armstrongmobile.nrfthingy52android.ble.fake

import com.armstrongmobile.nrfthingy52android.MainDispatcherRule
import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import com.armstrongmobile.nrfthingy52android.ui.detail.ConnectionState
import com.armstrongmobile.nrfthingy52android.ui.detail.ThingyConnectionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Exercises the fake transport itself — the substitute for the iOS CoreBluetoothMock layer. These
// cover the sensor-streaming and LED read-back paths the detail screen and dashboards (Phases 6–7)
// are built on; the full end-to-end pipeline suite lands in Phase 8.
class FakeThingyTransportTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun environmentReadingsStreamToViewModel() {
        val fake = FakeThingyController()
        val viewModel = ThingyConnectionViewModel(fake)
        assertFalse(viewModel.uiState.value.hasEnvironmentData)

        fake.simulateEnvironment(temperature = 22.5, humidity = 47, pressure = 1013.25, eco2 = 450, tvoc = 1200)

        val state = viewModel.uiState.value
        assertTrue(state.hasEnvironmentData)
        assertEquals(22.5, state.temperature!!, 0.001)
        assertEquals(47, state.humidity)
        assertEquals(1013.25, state.pressure!!, 0.001)
        assertEquals(450, state.eco2)
        assertEquals(1200, state.tvoc)
    }

    @Test
    fun motionReadingsStreamToViewModel() {
        val fake = FakeThingyController()
        val viewModel = ThingyConnectionViewModel(fake)
        assertFalse(viewModel.uiState.value.hasMotionData)

        fake.simulateOrientation(ThingyOrientation.REVERSE_PORTRAIT)
        fake.simulateTap(direction = TapDirection.Y_NEGATIVE, count = 3)
        fake.simulateStepCount(steps = 1234, durationSeconds = 56.789)
        fake.simulateHeading(degrees = 271.5)

        val state = viewModel.uiState.value
        assertTrue(state.hasMotionData)
        assertEquals(ThingyOrientation.REVERSE_PORTRAIT, state.orientation)
        assertEquals(TapDirection.Y_NEGATIVE, state.lastTapDirection)
        assertEquals(3, state.lastTapCount)
        assertEquals(1234, state.stepCount)
        assertEquals(56.789, state.stepDurationSeconds!!, 0.001)
        assertEquals(271.5, state.heading!!, 0.001)
    }

    // The optimistic write is confirmed by the firmware's read-back, mirroring the iOS
    // testLEDToggleWritesAndReadsBack assertion on ThingyMocks.ledIsOn.
    @Test
    fun ledToggleWritesAndReadsBack() {
        val fake = FakeThingyController()
        val viewModel = ThingyConnectionViewModel(fake)

        viewModel.setLed(on = true)
        assertTrue(fake.ledIsOn)
        assertTrue(viewModel.uiState.value.ledIsOn)

        viewModel.setLed(on = false)
        assertFalse(fake.ledIsOn)
        assertFalse(viewModel.uiState.value.ledIsOn)
    }

    @Test
    fun autoConnectDrivesConnectionLifecycle() {
        val fake = FakeThingyController(autoConnect = true)
        val viewModel = ThingyConnectionViewModel(fake)

        viewModel.connect()
        assertEquals(ConnectionState.CONNECTED, viewModel.uiState.value.connectionState)
        assertTrue(fake.isConnected)

        viewModel.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
        assertFalse(fake.isConnected)
    }

    @Test
    fun powerOffResolvesToDisconnected() {
        val fake = FakeThingyController(autoConnect = true)
        val viewModel = ThingyConnectionViewModel(fake)
        viewModel.connect()

        fake.powerOff()

        assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun fakeScannerReportsMockDeviceWhileScanning() = runTest {
        val scanner = FakeBleScanner()
        assertFalse(scanner.isScanning.value)

        val firstResult = async { scanner.scanResults.first() }
        runCurrent() // let the collector subscribe before the emission
        scanner.startScan()

        val result = firstResult.await()
        assertEquals(FakeBleScanner.MOCK_ADDRESS, result.address)
        assertEquals(ThingyMocks.MOCK_NAME, result.name)
        assertTrue(scanner.isScanning.value)

        scanner.stopScan()
        assertFalse(scanner.isScanning.value)
    }
}
