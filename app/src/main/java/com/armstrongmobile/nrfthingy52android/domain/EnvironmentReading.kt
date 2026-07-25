package com.armstrongmobile.nrfthingy52android.domain

// One decoded environment-sensor update (plan §3). A sealed interface is the Kotlin equivalent of
// the iOS `enum EnvironmentReading` with associated values; the data classes give value equality.
sealed interface EnvironmentReading {
    data class Temperature(val celsius: Double) : EnvironmentReading
    data class Humidity(val percent: Int) : EnvironmentReading
    data class Pressure(val hPa: Double) : EnvironmentReading
    data class AirQuality(val eco2: Int, val tvoc: Int) : EnvironmentReading
}
