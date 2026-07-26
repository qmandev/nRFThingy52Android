package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

// Every expectation here is the iOS SensorFormat.swift output table (plan §10 item 11, iOS reply §2)
// — units, decimal places, separators, grouping, and rounding must match exactly.
class SensorFormatTest {

    private val enUS = Locale.US
    private val deDE = Locale.GERMANY

    @Test
    fun temperatureUsesOneDecimalAndCelsius() {
        assertEquals("22.5 °C", SensorFormat.temperature(22.5, enUS))
        assertNull(SensorFormat.temperature(null))
    }

    @Test
    fun humidityIsWholePercent() {
        assertEquals("48 %", SensorFormat.humidity(48, enUS))
        assertNull(SensorFormat.humidity(null))
    }

    @Test
    fun pressureUsesOneDecimalAndHectopascals() {
        assertEquals("1013.2 hPa", SensorFormat.pressure(1013.25, enUS))
        assertNull(SensorFormat.pressure(null))
    }

    // iOS guards on both halves being present, so a partial reading shows the placeholder.
    @Test
    fun airQualityNeedsBothHalves() {
        assertEquals("450 ppm · 34 ppb", SensorFormat.airQuality(450, 34, enUS))
        assertNull(SensorFormat.airQuality(450, null))
        assertNull(SensorFormat.airQuality(null, 34))
    }

    @Test
    fun orientationAndStepsUseRawLabels() {
        assertEquals("Portrait (upside down)", SensorFormat.orientation(ThingyOrientation.REVERSE_PORTRAIT))
        assertNull(SensorFormat.orientation(null))
        assertEquals("1234", SensorFormat.steps(1234, enUS))
        assertNull(SensorFormat.steps(null))
    }

    @Test
    fun headingIsWholeDegrees() {
        assertEquals("272°", SensorFormat.heading(271.5, enUS))
        assertEquals("0°", SensorFormat.heading(0.0, enUS))
        assertNull(SensorFormat.heading(null))
    }

    @Test
    fun lastTapNeedsDirectionAndCount() {
        assertEquals("Z− · ×2", SensorFormat.lastTap(TapDirection.Z_NEGATIVE, 2, enUS))
        assertNull(SensorFormat.lastTap(TapDirection.Z_NEGATIVE, null))
        assertNull(SensorFormat.lastTap(null, 2))
    }

    // The whole point of the iOS fix: a comma-decimal locale gets a comma. Values are the iOS
    // reply's en_US/de_DE table.
    @Test
    fun decimalSeparatorFollowsTheLocale() {
        assertEquals("22,5 °C", SensorFormat.temperature(22.5, deDE))
        assertEquals("-5,2 °C", SensorFormat.temperature(-5.25, deDE))
        assertEquals("1013,2 hPa", SensorFormat.pressure(1013.25, deDE))

        // Readings with no fractional part are identical in both locales.
        assertEquals("272°", SensorFormat.heading(271.5, deDE))
        assertEquals("48 %", SensorFormat.humidity(48, deDE))
        assertEquals("450 ppm · 34 ppb", SensorFormat.airQuality(450, 34, deDE))
        assertEquals("Z− · ×2", SensorFormat.lastTap(TapDirection.Z_NEGATIVE, 2, deDE))
    }

    // Rounding is HALF_EVEN, matching Swift's FormatStyle. This is NOT what "%.1f" does: Java's
    // Formatter rounds HALF_UP and renders -5.25 as "-5.3", which would silently diverge from iOS.
    // Both half-way values are asserted because they are exactly where the two modes differ.
    @Test
    fun halfWayValuesRoundHalfEven() {
        assertEquals("-5.2 °C", SensorFormat.temperature(-5.25, enUS))
        assertEquals("1013.2 hPa", SensorFormat.pressure(1013.25, enUS))
        assertEquals("272°", SensorFormat.heading(271.5, enUS))
    }

    // Grouping is suppressed on every reading — "1450 ppm", never "1,450"/"1.450". Instrument values
    // in a monospaced column, and in de-DE a grouping dot reads as a decimal point. Cross-platform
    // decision: don't flip one side alone.
    @Test
    fun digitGroupingIsSuppressed() {
        assertEquals("1234", SensorFormat.steps(1234, enUS))
        assertEquals("1234", SensorFormat.steps(1234, deDE))
        assertEquals("1450 ppm · 1200 ppb", SensorFormat.airQuality(1450, 1200, deDE))
        assertEquals("1013,2 hPa", SensorFormat.pressure(1013.25, deDE))
    }

    // The UI passes no locale, so the default must be the device locale rather than a fixed one.
    @Test
    fun defaultLocaleIsTheDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(deDE)
            assertEquals("22,5 °C", SensorFormat.temperature(22.5))
            Locale.setDefault(enUS)
            assertEquals("22.5 °C", SensorFormat.temperature(22.5))
        } finally {
            Locale.setDefault(original)
        }
    }
}
