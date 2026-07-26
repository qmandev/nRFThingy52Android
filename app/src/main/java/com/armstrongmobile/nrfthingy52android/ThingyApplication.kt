package com.armstrongmobile.nrfthingy52android

import android.app.Application
import com.armstrongmobile.nrfthingy52android.ble.fake.ThingyMocks
import com.armstrongmobile.nrfthingy52android.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// Process init and owner of the composition root, the counterpart to iOS's @main ThingyApp entry
// point (plan §3). Holding AppContainer here gives the scanner and detail ViewModels one place to
// resolve their transport from, without a DI framework.
class ThingyApplication : Application() {

    lateinit var container: AppContainer
        private set

    // Process-lifetime scope for the fake transport's demo loops. SupervisorJob so one failing loop
    // can't cancel the other.
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Live demo readings so the mock build's dashboards are alive, mirroring iOS's
        // ThingyApp.init: simulator-only, and skipped under tests so the test drives the sensor
        // values itself (iOS checks XCTestConfigurationFilePath; the Android analogue is whether the
        // instrumentation test classes were loaded into this process).
        if (container.useFakeTransport && !isRunningUnderInstrumentation()) {
            ThingyMocks.startEnvironmentDemo(applicationScope)
            ThingyMocks.startMotionDemo(applicationScope)
        }
    }

    private fun isRunningUnderInstrumentation(): Boolean = runCatching {
        Class.forName("androidx.test.platform.app.InstrumentationRegistry")
        true
    }.getOrDefault(false)
}
