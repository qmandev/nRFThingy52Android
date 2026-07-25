package com.armstrongmobile.nrfthingy52android.ui.scanner

import com.armstrongmobile.nrfthingy52android.MainDispatcherRule
import com.armstrongmobile.nrfthingy52android.ble.fake.FakeBleScanner
import com.armstrongmobile.nrfthingy52android.ble.fake.ThingyMocks
import com.armstrongmobile.nrfthingy52android.domain.RssiBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Ports ScannerModelHelperTests from the iOS BLEModelTests.swift (plan §9.2).
class ScannerViewModelHelperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun advertisedNameUsesLocalName() {
        assertEquals(
            "My Thingy",
            ScannerViewModel.advertisedName("My Thingy", fallback = "Unknown Device"),
        )
    }

    @Test
    fun advertisedNameFallsBackForMissingName() {
        assertEquals("Unknown Device", ScannerViewModel.advertisedName(null, fallback = "Unknown Device"))
        // Android can also report an empty local name where iOS would report nil.
        assertEquals("Unknown Device", ScannerViewModel.advertisedName("", fallback = "Unknown Device"))
    }

    // iOS: shouldRefreshRow(lastUpdated: now - 0.5) == false, (now - 1.5) == true.
    @Test
    fun rowRefreshThrottle() {
        val now = 10_000L
        assertFalse(ScannerViewModel.shouldRefreshRow(lastUpdated = now - 500, now = now))
        assertTrue(ScannerViewModel.shouldRefreshRow(lastUpdated = now - 1_500, now = now))
    }

    // The dedupe half of iOS's handleDiscovery: repeated sightings of one address stay a single row.
    @Test
    fun repeatedSightingsDoNotDuplicateRows() {
        val scanner = FakeBleScanner()
        val viewModel = ScannerViewModel(scanner, unknownDeviceName = "Unknown Device")

        scanner.startScan()
        scanner.simulateDiscovery(rssi = -70)
        scanner.simulateDiscovery(rssi = -30)

        val discovered = viewModel.uiState.value.discovered
        assertEquals(1, discovered.size)
        assertEquals(FakeBleScanner.MOCK_ADDRESS, discovered.single().address)
        assertEquals(ThingyMocks.MOCK_NAME, discovered.single().name)
    }

    @Test
    fun firstSightingBucketsRssi() {
        val scanner = FakeBleScanner()
        val viewModel = ScannerViewModel(scanner, unknownDeviceName = "Unknown Device")

        scanner.startScan()

        // FakeBleScanner reports -45 dBm, which is the MEDIUM tier (< -40).
        assertEquals(RssiBucket.MEDIUM, viewModel.uiState.value.discovered.single().rssiBucket)
    }

    @Test
    fun clearDiscoveredEmptiesTheList() {
        val scanner = FakeBleScanner()
        val viewModel = ScannerViewModel(scanner, unknownDeviceName = "Unknown Device")
        scanner.startScan()
        assertEquals(1, viewModel.uiState.value.discovered.size)

        viewModel.clearDiscovered()

        assertTrue(viewModel.uiState.value.discovered.isEmpty())
    }

    @Test
    fun isScanningMirrorsTheScanner() {
        val scanner = FakeBleScanner()
        val viewModel = ScannerViewModel(scanner, unknownDeviceName = "Unknown Device")
        assertFalse(viewModel.uiState.value.isScanning)

        viewModel.startScan()
        assertTrue(viewModel.uiState.value.isScanning)

        viewModel.stopScan()
        assertFalse(viewModel.uiState.value.isScanning)
    }
}
