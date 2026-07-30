package com.armstrongmobile.nrfthingy52android.domain

// Device orientation (Motion service). Raw values 0–3 (plan §5.3).
//
// Deliberately carries no display label: these are localized (plan §10 item 13), and resolving a
// string resource here would give this pure-Kotlin domain type an Android dependency. The label
// lookup lives in the UI layer as `ThingyOrientation.labelRes` (ui/detail/OrientationLabel.kt), the
// same way RssiBucket maps to a drawable in ThingyRow rather than in the enum.
//
// iOS made the mirror-image change: its `label` became `labelKey`, resolved by the view.
enum class ThingyOrientation(val rawValue: Int) {
    PORTRAIT(0),
    LANDSCAPE(1),
    REVERSE_PORTRAIT(2),
    REVERSE_LANDSCAPE(3);

    companion object {
        // Null for anything outside 0–3, mirroring iOS `ThingyOrientation(rawValue:)`.
        fun fromRawValue(rawValue: Int): ThingyOrientation? = entries.firstOrNull { it.rawValue == rawValue }
    }
}
