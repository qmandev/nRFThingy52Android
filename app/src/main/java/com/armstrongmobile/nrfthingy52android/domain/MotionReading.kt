package com.armstrongmobile.nrfthingy52android.domain

// One decoded motion-sensor update (plan §3). Sealed interface = the iOS `enum MotionReading` with
// associated values.
sealed interface MotionReading {
    data class Tap(val direction: TapDirection, val count: Int) : MotionReading
    data class Orientation(val value: ThingyOrientation) : MotionReading
    data class StepCount(val steps: Int, val durationSeconds: Double) : MotionReading
    data class Heading(val degrees: Double) : MotionReading
}
