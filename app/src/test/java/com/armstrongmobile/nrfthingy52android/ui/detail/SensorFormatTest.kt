package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

// Every expected string here is the literal output of the iOS ThingyDetailView's format strings
// (plan §6.2) — units, decimal places and separators must match exactly.
class SensorFormatTest {

    @Test
    fun temperatureUsesOneDecimalAndCelsius() {
        assertEquals("22.5 °C", SensorFormat.temperature(22.5))
        assertEquals("-5.3 °C", SensorFormat.temperature(-5.25))
        assertNull(SensorFormat.temperature(null))
    }

    @Test
    fun humidityIsWholePercent() {
        assertEquals("47 %", SensorFormat.humidity(47))
        assertNull(SensorFormat.humidity(null))
    }

    @Test
    fun pressureUsesOneDecimalAndHectopascals() {
        assertEquals("1013.3 hPa", SensorFormat.pressure(1013.25))
        assertNull(SensorFormat.pressure(null))
    }

    // iOS guards on both halves being present, so a partial reading shows the placeholder.
    @Test
    fun airQualityNeedsBothHalves() {
        assertEquals("450 ppm · 1200 ppb", SensorFormat.airQuality(450, 1200))
        assertNull(SensorFormat.airQuality(450, null))
        assertNull(SensorFormat.airQuality(null, 1200))
    }

    @Test
    fun orientationAndStepsUseRawLabels() {
        assertEquals("Portrait (upside down)", SensorFormat.orientation(ThingyOrientation.REVERSE_PORTRAIT))
        assertNull(SensorFormat.orientation(null))
        assertEquals("1234", SensorFormat.steps(1234))
        assertNull(SensorFormat.steps(null))
    }

    @Test
    fun headingIsWholeDegrees() {
        assertEquals("272°", SensorFormat.heading(271.5))
        assertEquals("0°", SensorFormat.heading(0.0))
        assertNull(SensorFormat.heading(null))
    }

    @Test
    fun lastTapNeedsDirectionAndCount() {
        assertEquals("Y− · ×3", SensorFormat.lastTap(TapDirection.Y_NEGATIVE, 3))
        assertNull(SensorFormat.lastTap(TapDirection.Y_NEGATIVE, null))
        assertNull(SensorFormat.lastTap(null, 3))
    }

    // iOS's String(format:) is locale-independent and always emits a "." decimal separator; a
    // comma-decimal default locale must not change the reading.
    @Test
    fun decimalSeparatorIsLocaleIndependent() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("22.5 °C", SensorFormat.temperature(22.5))
            assertEquals("1013.3 hPa", SensorFormat.pressure(1013.25))
        } finally {
            Locale.setDefault(original)
        }
    }
}
