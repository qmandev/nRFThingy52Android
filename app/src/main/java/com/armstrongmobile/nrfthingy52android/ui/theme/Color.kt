package com.armstrongmobile.nrfthingy52android.ui.theme

import androidx.compose.ui.graphics.Color

// Nordic brand palette, ported verbatim from the iOS app's UIColorExtension.swift: each hex is the
// #colorLiteral RGB float ×255, rounded to the nearest integer (plan §7.1). Only nordicBlue
// (primary/accent) and nordicRed (error / disconnected-state tint) are consumed by the current UI;
// the rest is the full brand kit, kept for parity and future use.
object NordicColors {
    val nordicBlue = Color(0xFF00B7D7)
    val nordicSky = Color(0xFF7AD9E9)
    val nordicLake = Color(0xFF008CD2)
    val nordicLakeDark = Color(0xFF0079B7)
    val nordicBlueslate = Color(0xFF0049B0)
    val nordicLightGray = Color(0xFFE0E7E8)
    val nordicMediumGray = Color(0xFF8998A3)
    val nordicDarkGray = Color(0xFF42505A)
    val nordicGrass = Color(0xFFD8E200)
    val nordicSun = Color(0xFFFFD400)
    val nordicRed = Color(0xFFF44960)
    val nordicFall = Color(0xFFF99529)
}
