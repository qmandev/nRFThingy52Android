package com.armstrongmobile.nrfthingy52android.di

import android.content.Context
import com.armstrongmobile.nrfthingy52android.BuildConfig
import com.armstrongmobile.nrfthingy52android.ble.AndroidBleScanner
import com.armstrongmobile.nrfthingy52android.ble.BleScanner
import com.armstrongmobile.nrfthingy52android.ble.BluetoothStateObserver
import com.armstrongmobile.nrfthingy52android.ble.fake.ThingyMocks

// The composition root: the single place that decides fake vs. real BLE (plan §10.6, resolved as
// product flavors + lightweight manual DI — no Hilt/Koin for a single-module app).
//
// USE_FAKE_TRANSPORT comes from the `mock`/`prod` flavor, so the choice is explicit at build time.
// This is the closest Android analogue of the iOS app's compile-time
// #if targetEnvironment(simulator) switch: Android can't infer it from the environment, because some
// emulator images do provide a Bluetooth stack.
class AppContainer(context: Context) {

    val useFakeTransport: Boolean = BuildConfig.USE_FAKE_TRANSPORT

    val scanner: BleScanner =
        if (useFakeTransport) ThingyMocks.scanner else AndroidBleScanner(context)

    val thingyRepository: ThingyRepository =
        if (useFakeTransport) FakeThingyRepository() else RealThingyRepository(context)

    // The fake transport needs no permissions and no adapter, so the mock flavor reports Bluetooth as
    // permanently available — the same way the iOS simulator's mock manager is always poweredOn.
    val bluetoothStateObserver: BluetoothStateObserver? =
        if (useFakeTransport) null else BluetoothStateObserver(context)
}
