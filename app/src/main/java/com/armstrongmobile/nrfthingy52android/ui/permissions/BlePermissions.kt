package com.armstrongmobile.nrfthingy52android.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// The BLE runtime permissions, which differ across this app's minSdk 24 → targetSdk 36 range
// (plan §4.4):
//   • API 31+ : BLUETOOTH_SCAN + BLUETOOTH_CONNECT. The manifest declares SCAN with
//     neverForLocation, so no location permission is needed.
//   • API 24–30: scanning required ACCESS_FINE_LOCATION; BLUETOOTH/BLUETOOTH_ADMIN are normal
//     manifest-only permissions and are never requested at runtime.
object BlePermissions {
    val required: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun allGranted(context: Context): Boolean = required.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

// Tracks whether the BLE permissions are granted and requests them once.
//
// The request is triggered where the user first needs scanning — the scanner screen — rather than at
// app launch. That mirrors the intent of iOS creating its CBCentralManager lazily in
// ScannerModel.startScan() so the system prompt's timing matches first interaction, even though the
// mechanism (Activity Result API vs. implicit manager-creation prompt) differs entirely (plan §4.4).
class BlePermissionState(
    val granted: Boolean,
    val shouldShowRationale: Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberBlePermissionState(requestOnFirstAppearance: Boolean = true): BlePermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(BlePermissions.allGranted(context)) }
    var denied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
        denied = !granted
    }

    val request = { launcher.launch(BlePermissions.required.toTypedArray()) }

    LaunchedEffect(requestOnFirstAppearance) {
        if (requestOnFirstAppearance && !granted) request()
    }

    return BlePermissionState(granted = granted, shouldShowRationale = denied, request = request)
}
