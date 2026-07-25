package com.armstrongmobile.nrfthingy52android.domain

import org.junit.Assert.assertEquals
import org.junit.Test

// Ports RSSIBucketTests from the iOS BLEModelTests.swift (plan §9.2).
class RssiBucketTest {
    @Test
    fun bucketsAreMonotonicAcrossThresholds() {
        assertEquals(RssiBucket.WEAKEST, RssiBucket.of(-100))
        assertEquals(RssiBucket.WEAKEST, RssiBucket.of(-81))
        assertEquals(RssiBucket.WEAK, RssiBucket.of(-80))
        assertEquals(RssiBucket.WEAK, RssiBucket.of(-61))
        assertEquals(RssiBucket.MEDIUM, RssiBucket.of(-60))
        assertEquals(RssiBucket.MEDIUM, RssiBucket.of(-41))
        assertEquals(RssiBucket.STRONG, RssiBucket.of(-40))
        assertEquals(RssiBucket.STRONG, RssiBucket.of(-20))
    }

    // Adapted from testImageNamesMatchAssets: the domain layer exposes assetName instead of an
    // Android drawable id, keeping it framework-free (plan §9.2).
    @Test
    fun assetNamesMatchAssets() {
        assertEquals("rssi_1", RssiBucket.WEAKEST.assetName)
        assertEquals("rssi_2", RssiBucket.WEAK.assetName)
        assertEquals("rssi_3", RssiBucket.MEDIUM.assetName)
        assertEquals("rssi_4", RssiBucket.STRONG.assetName)
    }
}
