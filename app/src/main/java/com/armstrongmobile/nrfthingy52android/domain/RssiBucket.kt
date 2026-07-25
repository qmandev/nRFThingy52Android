package com.armstrongmobile.nrfthingy52android.domain

// Signal-strength bucket for the scanner list. Four tiers with the exact thresholds ported from the
// iOS RSSIBucket (plan §3): < -80 WEAKEST, < -60 WEAK, < -40 MEDIUM, else STRONG. `assetName`
// mirrors the iOS `imageName` rawValue (the rssi_1..rssi_4 drawables land in Phase 5); it is kept as
// a plain String so the domain layer stays free of Android resource dependencies.
enum class RssiBucket(val tier: Int, val assetName: String) {
    WEAKEST(1, "rssi_1"),
    WEAK(2, "rssi_2"),
    MEDIUM(3, "rssi_3"),
    STRONG(4, "rssi_4");

    companion object {
        fun of(rssi: Int): RssiBucket = when {
            rssi < -80 -> WEAKEST
            rssi < -60 -> WEAK
            rssi < -40 -> MEDIUM
            else -> STRONG
        }
    }
}
