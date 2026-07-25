package com.armstrongmobile.nrfthingy52android.domain

// Device orientation (Motion service). Raw values 0–3, labels ported verbatim from the iOS
// ThingyOrientation (plan §5.3).
enum class ThingyOrientation(val rawValue: Int, val label: String) {
    PORTRAIT(0, "Portrait"),
    LANDSCAPE(1, "Landscape"),
    REVERSE_PORTRAIT(2, "Portrait (upside down)"),
    REVERSE_LANDSCAPE(3, "Landscape (upside down)");

    companion object {
        // Null for anything outside 0–3, mirroring iOS `ThingyOrientation(rawValue:)`.
        fun fromRawValue(rawValue: Int): ThingyOrientation? = entries.firstOrNull { it.rawValue == rawValue }
    }
}
