package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import java.util.Locale

// Value formatting for the sensor dashboard rows, matching the iOS ThingyDetailView's format strings
// exactly — units, decimal places and separators (plan §6.2). Pure functions so the formats are
// unit-testable without a Compose or Android context.
//
// DO NOT "fix" the locale handling here. Locale.ROOT is deliberate parity, not an oversight: iOS's
// String(format:) without an explicit locale always emits a "." decimal separator, so a German user
// sees "22.5 °C" on iOS today. Formatting with the device locale here would print "22,5 °C" and make
// the two apps render different text from identical sensor bytes.
//
// This is a known defect **in the iOS app** — see plan §10 item 11. The fix belongs there
// (String(format:locale:) or a FormatStyle/NumberFormatter); when iOS lands it, mirror it here by
// switching to the default locale and inverting
// SensorFormatTest.decimalSeparatorIsLocaleIndependent. Only the numeric values are affected — the
// row labels localize normally through stringResource.
object SensorFormat {

    // Placeholder shown until the first reading of that kind arrives (iOS: `value ?? "—"`).
    const val PLACEHOLDER = "—" // EM DASH

    fun temperature(celsius: Double?): String? =
        celsius?.let { String.format(Locale.ROOT, "%.1f °C", it) }

    fun humidity(percent: Int?): String? = percent?.let { "$it %" }

    fun pressure(hPa: Double?): String? =
        hPa?.let { String.format(Locale.ROOT, "%.1f hPa", it) }

    // Null until *both* halves of the air-quality reading have arrived, matching iOS's guard.
    fun airQuality(eco2: Int?, tvoc: Int?): String? {
        if (eco2 == null || tvoc == null) return null
        return "$eco2 ppm · $tvoc ppb" // MIDDLE DOT separator
    }

    fun orientation(orientation: ThingyOrientation?): String? = orientation?.label

    fun steps(stepCount: Int?): String? = stepCount?.toString()

    fun heading(degrees: Double?): String? =
        degrees?.let { String.format(Locale.ROOT, "%.0f°", it) } // DEGREE SIGN

    // Null until a tap has been observed; "X+ · ×3" (MIDDLE DOT + MULTIPLICATION SIGN).
    fun lastTap(direction: TapDirection?, count: Int?): String? {
        if (direction == null || count == null) return null
        return "${direction.label} · ×$count"
    }
}
