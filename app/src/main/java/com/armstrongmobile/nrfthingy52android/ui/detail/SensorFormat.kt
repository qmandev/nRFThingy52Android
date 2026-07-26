package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

// Value formatting for the sensor dashboard rows, mirroring the iOS SensorFormat.swift contract
// (plan §6.2, §10 item 11). Pure functions so the formats are unit-testable without a Compose or
// Android context.
//
// Readings are locale-aware: iOS moved off the locale-independent String(format:) to a FormatStyle,
// so a German user now correctly sees "22,5 °C". Android matches by formatting through NumberFormat
// with the caller's locale. Every reading lives here — including the integer ones that were correct
// by accident — so the next reading someone adds inherits the right behavior.
//
// Three details are load-bearing for parity; do not change one side without the other:
//
//  1. GROUPING IS SUPPRESSED EVERYWHERE. Pressure/eco2/steps above 999 render "1450 ppm", never
//     "1,450"/"1.450". These are instrument values in a monospaced-digit column, and in de-DE a
//     grouping dot reads as a decimal point to exactly the users the locale fix was for. Flipping
//     this is a cross-platform decision.
//  2. ROUNDING IS HALF_EVEN, matching Swift's FormatStyle. This is *not* what Kotlin's
//     String.format/"%.1f" does — java.util.Formatter rounds HALF_UP, which renders -5.25 as "-5.3"
//     where iOS renders "-5.2". That is why this goes through NumberFormat (whose default is
//     HALF_EVEN) and why SensorFormatTest asserts the half-way values explicitly.
//  3. The separators in composite readings are fixed characters, not locale-derived: "·" is U+00B7,
//     "×" is U+00D7, and the tap direction's minus is U+2212 (from TapDirection.label).
object SensorFormat {

    // Placeholder shown until the first reading of that kind arrives (iOS: `value ?? "—"`).
    const val PLACEHOLDER = "—" // EM DASH

    fun temperature(celsius: Double?, locale: Locale = Locale.getDefault()): String? =
        celsius?.let { "${decimal(it, fractionDigits = 1, locale = locale)} °C" }

    fun humidity(percent: Int?, locale: Locale = Locale.getDefault()): String? =
        percent?.let { "${integer(it, locale)} %" }

    fun pressure(hPa: Double?, locale: Locale = Locale.getDefault()): String? =
        hPa?.let { "${decimal(it, fractionDigits = 1, locale = locale)} hPa" }

    // Null until *both* halves of the air-quality reading have arrived, matching iOS's guard.
    fun airQuality(eco2: Int?, tvoc: Int?, locale: Locale = Locale.getDefault()): String? {
        if (eco2 == null || tvoc == null) return null
        return "${integer(eco2, locale)} ppm · ${integer(tvoc, locale)} ppb"
    }

    fun orientation(orientation: ThingyOrientation?): String? = orientation?.label

    fun steps(stepCount: Int?, locale: Locale = Locale.getDefault()): String? =
        stepCount?.let { integer(it, locale) }

    fun heading(degrees: Double?, locale: Locale = Locale.getDefault()): String? =
        degrees?.let { "${decimal(it, fractionDigits = 0, locale = locale)}°" } // DEGREE SIGN

    // Null until a tap has been observed; "Z− · ×2".
    fun lastTap(direction: TapDirection?, count: Int?, locale: Locale = Locale.getDefault()): String? {
        if (direction == null || count == null) return null
        return "${direction.label} · ×${integer(count, locale)}"
    }

    private fun decimal(value: Double, fractionDigits: Int, locale: Locale): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_EVEN
        }.format(value)

    private fun integer(value: Int, locale: Locale): String =
        NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }.format(value)
}
