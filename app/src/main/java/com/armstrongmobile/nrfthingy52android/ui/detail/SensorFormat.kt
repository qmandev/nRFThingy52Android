package com.armstrongmobile.nrfthingy52android.ui.detail

import com.armstrongmobile.nrfthingy52android.domain.TapDirection
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation
import java.util.Locale

// Value formatting for the sensor dashboard rows, matching the iOS ThingyDetailView's format strings
// exactly — units, decimal places and separators (plan §6.2). Pure functions so the formats are
// unit-testable without a Compose or Android context.
//
// Locale.ROOT is deliberate: iOS's String(format:) without an explicit locale is locale-independent
// and always emits a "." decimal separator, so formatting with the device locale here would diverge
// from the iOS output (e.g. "22,5 °C" in de-DE). Only the *numbers* are locale-independent; the row
// labels themselves are localized through stringResource.
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
