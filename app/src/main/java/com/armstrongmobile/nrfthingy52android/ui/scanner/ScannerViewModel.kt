package com.armstrongmobile.nrfthingy52android.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.armstrongmobile.nrfthingy52android.ble.BleScanner
import com.armstrongmobile.nrfthingy52android.ble.ThingyScanResult
import com.armstrongmobile.nrfthingy52android.domain.RssiBucket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One discovered Thingy row. Holds no BluetoothDevice or controller reference — the address is the
// only thing that travels to the detail screen, since Navigation Compose routes carry primitives
// (plan §3, §6.1).
data class DiscoveredThingyUi(
    val address: String,
    val name: String,
    val rssiBucket: RssiBucket,
    val lastUpdated: Long,
)

data class ScannerUiState(
    val discovered: List<DiscoveredThingyUi> = emptyList(),
    val isScanning: Boolean = false,
)

// Scanner state for the Compose UI — the port of the iOS ScannerModel, minus the central-manager
// ownership iOS needs (each Android connection gets its own callback object, so there is no
// sole-delegate forwarding layer to reproduce; plan §4 point 6).
//
// CONCURRENCY BOUNDARY (plan §4.1): the scanner emits sightings from a Binder thread; the collection
// below in viewModelScope is the only place uiState is mutated.
class ScannerViewModel(
    private val scanner: BleScanner,
    // Injected rather than resolved here: a ViewModel has no Composable context and Android has no
    // global Bundle.main equivalent for string lookup (plan §3).
    private val unknownDeviceName: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanner.scanResults.collect(::onScanResult)
        }
        viewModelScope.launch {
            scanner.isScanning.collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
    }

    fun startScan() = scanner.startScan()

    fun stopScan() = scanner.stopScan()

    fun clearDiscovered() = _uiState.update { it.copy(discovered = emptyList()) }

    // Dedupe by address (Android's analogue of CBPeripheral.identifier) and throttle visible row
    // updates to once per second, ported from iOS's handleDiscovery (plan §4 point 8).
    private fun onScanResult(result: ThingyScanResult, now: Long = System.currentTimeMillis()) {
        val name = advertisedName(result.name, unknownDeviceName)
        val bucket = RssiBucket.of(result.rssi)

        _uiState.update { state ->
            val index = state.discovered.indexOfFirst { it.address == result.address }
            when {
                index < 0 -> state.copy(
                    discovered = state.discovered + DiscoveredThingyUi(
                        address = result.address,
                        name = name,
                        rssiBucket = bucket,
                        lastUpdated = now,
                    )
                )

                !shouldRefreshRow(state.discovered[index].lastUpdated, now) -> state

                else -> state.copy(
                    discovered = state.discovered.toMutableList().also { rows ->
                        rows[index] = rows[index].copy(
                            name = name,
                            rssiBucket = bucket,
                            lastUpdated = now,
                        )
                    }
                )
            }
        }
    }

    companion object {
        // iOS: ScannerModel.rowUpdateInterval = 1.0 s.
        const val ROW_UPDATE_INTERVAL_MS = 1_000L

        // Pure helpers, kept static and framework-free so they port the iOS ScannerModel statics
        // 1:1 and stay directly unit-testable (plan §9.2).

        // The advertised local name, falling back to the localized "Unknown Device" — iOS's
        // advertisedName(from:), which reads CBAdvertisementDataLocalNameKey.
        fun advertisedName(advertisedName: String?, fallback: String): String =
            advertisedName?.takeIf { it.isNotEmpty() } ?: fallback

        // Whether a row last updated at `lastUpdated` should refresh now. Strictly greater than the
        // interval, matching iOS's `now.timeIntervalSince(lastUpdated) > rowUpdateInterval`.
        fun shouldRefreshRow(lastUpdated: Long, now: Long): Boolean =
            now - lastUpdated > ROW_UPDATE_INTERVAL_MS

        fun factory(scanner: BleScanner, unknownDeviceName: String) = viewModelFactory {
            initializer { ScannerViewModel(scanner, unknownDeviceName) }
        }
    }
}
