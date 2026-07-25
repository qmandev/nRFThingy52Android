package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.MainDispatcherRule
import com.armstrongmobile.nrfthingy52android.ble.fake.FakeThingyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Ports ThingyConnectionTests from the iOS BLEModelTests.swift (plan §9.2), including its makeSUT()
// factory pattern. The fake is left in non-autoConnect mode so the lifecycle is driven explicitly,
// matching the dumb `MockThingy` recorder the iOS suite uses.
class ThingyConnectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun makeSUT(): Pair<FakeThingyController, ThingyConnectionViewModel> {
        val fake = FakeThingyController(advertisedName = "Mock Thingy")
        return fake to ThingyConnectionViewModel(fake)
    }

    @Test
    fun initSubscribesToEventsAndStartsConnecting() {
        val (fake, viewModel) = makeSUT()
        assertEquals(ConnectionState.CONNECTING, viewModel.uiState.value.connectionState)
        // Proves the init-time collection is live: an event emitted now is observed.
        fake.pressButton()
        assertTrue(viewModel.uiState.value.buttonPressed)
    }

    @Test
    fun connectCallsPeripheralWhenDisconnected() {
        val (fake, viewModel) = makeSUT()
        viewModel.connect()
        assertEquals(1, fake.connectCalls)
    }

    @Test
    fun connectSkipsWhenAlreadyConnected() {
        val (fake, viewModel) = makeSUT()
        fake.simulateConnected()
        viewModel.connect()
        assertEquals(0, fake.connectCalls)
    }

    @Test
    fun didConnectPublishesSupportFlags() {
        val (fake, viewModel) = makeSUT()
        fake.simulateConnected(ledSupported = true, buttonSupported = true)
        val state = viewModel.uiState.value
        assertEquals(ConnectionState.CONNECTED, state.connectionState)
        assertTrue(state.ledSupported)
        assertTrue(state.buttonSupported)
        assertEquals(0, fake.disconnectCalls)
    }

    @Test
    fun didConnectWithNoSupportedFeaturesDisconnects() {
        val (fake, _) = makeSUT()
        fake.simulateConnected(ledSupported = false, buttonSupported = false)
        assertEquals(1, fake.disconnectCalls)
    }

    @Test
    fun didDisconnectPublishesDisconnectedState() {
        val (fake, viewModel) = makeSUT()
        fake.simulateConnected(ledSupported = true, buttonSupported = true)
        fake.simulateDisconnection()
        assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }

    @Test
    fun ledStateChangesArePublished() {
        val (fake, viewModel) = makeSUT()
        fake.turnOnLed()
        assertTrue(viewModel.uiState.value.ledIsOn)
        fake.turnOffLed()
        assertFalse(viewModel.uiState.value.ledIsOn)
    }

    @Test
    fun buttonStateChangesArePublished() {
        val (fake, viewModel) = makeSUT()
        fake.pressButton()
        assertTrue(viewModel.uiState.value.buttonPressed)
        fake.releaseButton()
        assertFalse(viewModel.uiState.value.buttonPressed)
    }

    @Test
    fun setLedIsOptimisticAndForwards() {
        val (fake, viewModel) = makeSUT()
        viewModel.setLed(on = true)
        assertTrue(viewModel.uiState.value.ledIsOn)
        assertEquals(1, fake.turnOnCalls)

        viewModel.setLed(on = false)
        assertFalse(viewModel.uiState.value.ledIsOn)
        assertEquals(1, fake.turnOffCalls)
    }

    @Test
    fun nameFallsBackWhenPeripheralHasNoName() {
        val (fake, viewModel) = makeSUT()
        assertEquals("Mock Thingy", viewModel.name)
        fake.advertisedName = null
        assertEquals("Unknown Device", viewModel.name)
    }
}
