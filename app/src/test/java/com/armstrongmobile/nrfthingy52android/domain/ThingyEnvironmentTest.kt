package com.armstrongmobile.nrfthingy52android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Ports ThingyEnvironmentTests from the iOS BLEModelTests.swift (plan §9.2), fixtures verbatim.
class ThingyEnvironmentTest {
    @Test
    fun temperatureParsingPositiveAndNegative() {
        assertEquals(
            EnvironmentReading.Temperature(22.5),
            ThingyEnvironment.parseTemperature(byteArrayOf(22, 50)),
        )
        assertEquals(
            EnvironmentReading.Temperature(-5.25),
            ThingyEnvironment.parseTemperature(byteArrayOf(-5, 25)),
        )
        assertNull(ThingyEnvironment.parseTemperature(byteArrayOf(1)))
    }

    @Test
    fun pressureParsing() {
        assertEquals(
            EnvironmentReading.Pressure(1013.25),
            ThingyEnvironment.parsePressure(ThingyEnvironment.encodePressure(1013.25)),
        )
        assertNull(ThingyEnvironment.parsePressure(byteArrayOf(0, 0, 0)))
    }

    @Test
    fun humidityParsing() {
        assertEquals(
            EnvironmentReading.Humidity(47),
            ThingyEnvironment.parseHumidity(byteArrayOf(47)),
        )
        assertNull(ThingyEnvironment.parseHumidity(byteArrayOf()))
    }

    @Test
    fun airQualityParsing() {
        assertEquals(
            EnvironmentReading.AirQuality(450, 1200),
            ThingyEnvironment.parseAirQuality(ThingyEnvironment.encodeAirQuality(450, 1200)),
        )
        assertNull(ThingyEnvironment.parseAirQuality(byteArrayOf(1, 2)))
    }

    @Test
    fun temperatureEncodingRoundTrip() {
        assertEquals(
            EnvironmentReading.Temperature(22.5),
            ThingyEnvironment.parseTemperature(ThingyEnvironment.encodeTemperature(22.5)),
        )
    }
}
