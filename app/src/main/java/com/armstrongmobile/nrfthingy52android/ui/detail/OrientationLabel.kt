package com.armstrongmobile.nrfthingy52android.ui.detail

import androidx.annotation.StringRes
import com.armstrongmobile.nrfthingy52android.R
import com.armstrongmobile.nrfthingy52android.domain.ThingyOrientation

// Maps an orientation to its localized label (plan §10 item 13, resolved 2026-07-28).
//
// This lives in the UI layer rather than on the enum so `domain/` keeps its zero-framework-dependency
// property — the same split as RssiBucket -> drawable in ThingyRow. The iOS side made the matching
// change: `ThingyOrientation.label` became `labelKey`, resolved by the view.
//
// Unlike TapDirection's `X+` / `Z−`, these are words rather than symbols, so they are translated in
// all 17 non-English locales. The terms are each platform's established *device*-orientation
// vocabulary, not literal translations of the English pair — German uses Hochformat/Querformat
// ("high format"/"cross format"), Portuguese Retrato/Paisagem. See the plan for the framework-res
// evidence behind that choice.
@get:StringRes
val ThingyOrientation.labelRes: Int
    get() = when (this) {
        ThingyOrientation.PORTRAIT -> R.string.orientation_portrait
        ThingyOrientation.LANDSCAPE -> R.string.orientation_landscape
        ThingyOrientation.REVERSE_PORTRAIT -> R.string.orientation_portrait_upside_down
        ThingyOrientation.REVERSE_LANDSCAPE -> R.string.orientation_landscape_upside_down
    }
