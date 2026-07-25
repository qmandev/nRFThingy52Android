package com.armstrongmobile.nrfthingy52android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.armstrongmobile.nrfthingy52android.di.AppContainer
import com.armstrongmobile.nrfthingy52android.ui.detail.ThingyConnectionViewModel
import com.armstrongmobile.nrfthingy52android.ui.detail.ThingyDetailScreen
import com.armstrongmobile.nrfthingy52android.ui.permissions.rememberBlePermissionState
import com.armstrongmobile.nrfthingy52android.ui.scanner.ScannerScreen
import com.armstrongmobile.nrfthingy52android.ui.scanner.ScannerViewModel
import com.armstrongmobile.nrfthingy52android.ui.theme.ThingyTheme

// Single-Activity Compose entry point (plan §3), the counterpart to iOS's @main ThingyApp +
// WindowGroup { ScannerView() }.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ThingyApplication).container
        setContent {
            ThingyTheme {
                ThingyNavHost(container)
            }
        }
    }
}

private object Routes {
    const val SCANNER = "scanner"
    const val DETAIL = "detail/{deviceAddress}"
    const val ARG_DEVICE_ADDRESS = "deviceAddress"

    fun detail(deviceAddress: String) = "detail/$deviceAddress"
}

@Composable
private fun ThingyNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SCANNER) {
        composable(Routes.SCANNER) {
            val unknownDeviceName = stringResource(R.string.unknown_device)
            val viewModel: ScannerViewModel = viewModel(
                factory = ScannerViewModel.factory(container.scanner, unknownDeviceName)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // The real transport can't scan without permission; the fake needs none, so the mock
            // flavor skips the prompt entirely (plan §4.4 — request at first scanner appearance,
            // matching the timing intent of iOS creating its central manager lazily).
            val permissions = rememberBlePermissionState(
                requestOnFirstAppearance = !container.useFakeTransport
            )
            val mayScan = container.useFakeTransport || permissions.granted

            ScannerScreen(
                uiState = uiState,
                onDeviceSelected = { address ->
                    viewModel.stopScan()
                    navController.navigate(Routes.detail(address))
                },
                onStartScan = {
                    if (mayScan) {
                        viewModel.clearDiscovered()
                        viewModel.startScan()
                    }
                },
            )
        }

        // The route carries only the MAC address: Navigation Compose passes primitives, so the detail
        // ViewModel resolves the peripheral through ThingyRepository rather than receiving an object
        // reference the way iOS's ThingyConnection(peripheral:) does (plan §6.1).
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_DEVICE_ADDRESS) { type = NavType.StringType }),
        ) {
            val unknownDeviceName = stringResource(R.string.unknown_device)
            val viewModel: ThingyConnectionViewModel = viewModel(
                factory = ThingyConnectionViewModel.factory(container.thingyRepository, unknownDeviceName)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ThingyDetailScreen(
                uiState = uiState,
                onLedToggle = viewModel::setLed,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
            )
        }
    }
}
