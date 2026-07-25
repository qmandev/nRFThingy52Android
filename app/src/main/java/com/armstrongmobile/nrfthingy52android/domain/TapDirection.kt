package com.armstrongmobile.nrfthingy52android.domain

// Tap direction (Motion service). Raw values 1–6 match the Thingy:52 firmware and the iOS
// TapDirection. Labels are ported verbatim — the negatives use the U+2212 MINUS SIGN, not an ASCII
// hyphen (plan §5.3), so keep the escape.
enum class TapDirection(val rawValue: Int, val label: String) {
    X_POSITIVE(1, "X+"),
    X_NEGATIVE(2, "X−"),
    Y_POSITIVE(3, "Y+"),
    Y_NEGATIVE(4, "Y−"),
    Z_POSITIVE(5, "Z+"),
    Z_NEGATIVE(6, "Z−");

    companion object {
        // Returns null for 0 or any value outside 1–6, mirroring the iOS `TapDirection(rawValue:)`.
        fun fromRawValue(rawValue: Int): TapDirection? = entries.firstOrNull { it.rawValue == rawValue }
    }
}
