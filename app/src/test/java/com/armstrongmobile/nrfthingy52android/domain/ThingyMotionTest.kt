package com.armstrongmobile.nrfthingy52android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ports ThingyMotionTests from the iOS BLEModelTests.swift (plan §9.2), fixtures verbatim.
class ThingyMotionTest {
    @Test
    fun tapParsing() {
        assertEquals(
            MotionReading.Tap(TapDirection.Y_NEGATIVE, 3),
            ThingyMotion.parseTap(byteArrayOf(TapDirection.Y_NEGATIVE.rawValue.toByte(), 3)),
        )
        assertNull(ThingyMotion.parseTap(byteArrayOf(1)))
        assertNull(ThingyMotion.parseTap(byteArrayOf(0, 1))) // 0 is not a valid direction
    }

    @Test
    fun orientationParsing() {
        assertEquals(
            MotionReading.Orientation(ThingyOrientation.REVERSE_PORTRAIT),
            ThingyMotion.parseOrientation(byteArrayOf(2)),
        )
        assertNull(ThingyMotion.parseOrientation(byteArrayOf(4)))
        assertNull(ThingyMotion.parseOrientation(byteArrayOf()))
    }

    @Test
    fun stepCountRoundTrip() {
        val reading = ThingyMotion.parseStepCount(ThingyMotion.encodeStepCount(1234, 56.789))
        assertTrue(reading is MotionReading.StepCount)
        reading as MotionReading.StepCount
        assertEquals(1234, reading.steps)
        assertEquals(56.789, reading.durationSeconds, 0.001)
        assertNull(ThingyMotion.parseStepCount(byteArrayOf(0, 0, 0)))
    }

    @Test
    fun headingRoundTrip() {
        assertEquals(
            MotionReading.Heading(271.5),
            ThingyMotion.parseHeading(ThingyMotion.encodeHeading(271.5)),
        )
        assertNull(ThingyMotion.parseHeading(byteArrayOf(1, 2)))
    }

    @Test
    fun tapDirectionLabels() {
        assertEquals("X+", TapDirection.X_POSITIVE.label)
        assertEquals("Z−", TapDirection.Z_NEGATIVE.label)
    }
}
