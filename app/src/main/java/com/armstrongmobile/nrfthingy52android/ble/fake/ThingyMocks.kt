package com.armstrongmobile.nrfthingy52android.ble.fake

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

// Seeds and drives the simulated Thingy:52, the counterpart to the iOS `ThingyMocks` facade
// (plan §9.1). The mock flavor's composition root hands these instances to the ViewModels
// (BuildConfig.USE_FAKE_TRANSPORT, plan §10.6); tests can also use them directly.
//
// Method names mirror the iOS facade so the two test suites read alike. Unlike iOS — where
// ThingyMocks is a global enum seeding a process-wide CoreBluetoothMock manager — the demo loops
// here take an explicit CoroutineScope, because Android has no equivalent global simulation state
// and structured concurrency should own the timers.
object ThingyMocks {

    // Matches the iOS ThingyMocks.mockName verbatim: the Compose UI test asserts on this text, the
    // way the XCUITest asserts app.staticTexts["Thingy52 Mock"].
    const val MOCK_NAME = "Thingy52 Mock"

    // A shared instance pair so the scanner and the detail screen agree on one simulated device.
    val controller: FakeThingyController by lazy {
        FakeThingyController(advertisedName = MOCK_NAME, autoConnect = true)
    }

    val scanner: FakeBleScanner by lazy { FakeBleScanner() }

    // Drifting demo readings so the dashboards are alive in the mock build, mirroring the iOS
    // startEnvironmentDemo() timer.
    fun startEnvironmentDemo(scope: CoroutineScope): Job = scope.launch {
        var temperature = 22.5
        var humidity = 45.0
        var pressure = 1013.2
        var eco2 = 480.0
        var tvoc = 18.0
        while (isActive) {
            temperature = (temperature + Random.nextDouble(-0.3, 0.3)).coerceIn(15.0, 30.0)
            humidity = (humidity + Random.nextDouble(-1.0, 1.0)).coerceIn(25.0, 70.0)
            pressure = (pressure + Random.nextDouble(-0.5, 0.5)).coerceIn(980.0, 1040.0)
            eco2 = (eco2 + Random.nextDouble(-20.0, 20.0)).coerceIn(400.0, 1200.0)
            tvoc = (tvoc + Random.nextDouble(-4.0, 4.0)).coerceIn(0.0, 120.0)
            controller.simulateEnvironment(
                temperature = temperature,
                humidity = humidity.toInt(),
                pressure = pressure,
                eco2 = eco2.toInt(),
                tvoc = tvoc.toInt(),
            )
            delay(ENVIRONMENT_DEMO_INTERVAL_MS)
        }
    }

    // Mirrors the iOS startMotionDemo() timer, with one deliberate demo-only difference: orientation
    // is re-emitted on every tick and a tap is simulated periodically. iOS emits orientation just once
    // at loop start and never taps, which means a screen opened later never sees those two values —
    // the events are already gone, since readings are not replayed to late subscribers. Re-emitting
    // keeps all four Motion rows alive for interactive use and on-device format checks. This affects
    // demo content only; no parsing, gating, or formatting behavior differs from iOS.
    fun startMotionDemo(scope: CoroutineScope): Job = scope.launch {
        var steps = 0
        var headingDegrees = 0.0
        var elapsedSeconds = 0.0
        var orientationIndex = 0
        var tickCount = 0
        while (isActive) {
            steps += Random.nextInt(0, 5)
            headingDegrees = (headingDegrees + Random.nextDouble(-15.0, 15.0)) % 360
            if (headingDegrees < 0) headingDegrees += 360

            controller.simulateOrientation(ThingyOrientation.entries[orientationIndex])
            controller.simulateStepCount(steps = steps, durationSeconds = elapsedSeconds)
            controller.simulateHeading(degrees = headingDegrees)
            if (tickCount % TAP_EVERY_N_TICKS == 0) {
                controller.simulateTap(
                    direction = TapDirection.entries.random(),
                    count = Random.nextInt(1, 4),
                )
            }

            delay(MOTION_DEMO_INTERVAL_MS)
            elapsedSeconds += MOTION_DEMO_INTERVAL_MS / 1000.0
            tickCount++
            // Drift slowly through the orientations rather than flipping every tick.
            if (tickCount % ORIENTATION_EVERY_N_TICKS == 0) {
                orientationIndex = (orientationIndex + 1) % ThingyOrientation.entries.size
            }
        }
    }

    private const val ENVIRONMENT_DEMO_INTERVAL_MS = 2_000L
    private const val MOTION_DEMO_INTERVAL_MS = 3_000L
    private const val TAP_EVERY_N_TICKS = 3
    private const val ORIENTATION_EVERY_N_TICKS = 4
}
