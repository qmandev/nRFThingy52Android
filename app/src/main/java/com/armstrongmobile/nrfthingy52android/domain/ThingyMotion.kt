package com.armstrongmobile.nrfthingy52android.domain

import java.util.UUID
import kotlin.math.roundToInt

// Thingy:52 Motion service (EF680400-…): the practical single-value subset — Tap, Orientation, Step
// Counter, Heading (plan §5.3). Quaternion/Euler/rotation-matrix/raw-accelerometer characteristics
// (UUID suffixes 0401/0404/0406–0408) are deferred, matching the iOS app (plan §10.9). Ported 1:1
// from the iOS ThingyMotion.swift; no Android dependencies.
object ThingyMotion {
    val serviceUuid: UUID = UUID.fromString("EF680400-9B35-4933-9B10-52FFA9740042")
    val tapCharacteristicUuid: UUID = UUID.fromString("EF680402-9B35-4933-9B10-52FFA9740042")
    val orientationCharacteristicUuid: UUID = UUID.fromString("EF680403-9B35-4933-9B10-52FFA9740042")
    val stepCounterCharacteristicUuid: UUID = UUID.fromString("EF680405-9B35-4933-9B10-52FFA9740042")
    val headingCharacteristicUuid: UUID = UUID.fromString("EF680409-9B35-4933-9B10-52FFA9740042")

    // byte 0: direction 0x01–0x06; byte 1: uint8 count. Null on a short buffer or an invalid direction.
    fun parseTap(data: ByteArray): MotionReading? {
        if (data.size < 2) return null
        val direction = TapDirection.fromRawValue(data[0].toInt() and 0xFF) ?: return null
        return MotionReading.Tap(direction, data[1].toInt() and 0xFF)
    }

    // single byte 0x00–0x03.
    fun parseOrientation(data: ByteArray): MotionReading? {
        if (data.isEmpty()) return null
        val orientation = ThingyOrientation.fromRawValue(data[0].toInt() and 0xFF) ?: return null
        return MotionReading.Orientation(orientation)
    }

    // uint32 LE steps + uint32 LE milliseconds → seconds.
    fun parseStepCount(data: ByteArray): MotionReading? {
        if (data.size < 8) return null
        val steps = readUInt32Le(data, 0)
        val millis = readUInt32Le(data, 4)
        return MotionReading.StepCount(steps.toInt(), millis / 1000.0)
    }

    // int32 LE, 16Q16 fixed-point degrees.
    fun parseHeading(data: ByteArray): MotionReading? {
        if (data.size < 4) return null
        val raw = readInt32Le(data, 0)
        return MotionReading.Heading(raw / 65536.0)
    }

    fun encodeTap(direction: TapDirection, count: Int): ByteArray =
        byteArrayOf(direction.rawValue.toByte(), count.coerceIn(0, 255).toByte())

    fun encodeOrientation(orientation: ThingyOrientation): ByteArray = byteArrayOf(orientation.rawValue.toByte())

    fun encodeStepCount(steps: Int, durationSeconds: Double): ByteArray =
        uint32Le(steps.toLong().coerceAtLeast(0)) + uint32Le((durationSeconds * 1000).toLong().coerceAtLeast(0))

    fun encodeHeading(degrees: Double): ByteArray = int32Le((degrees * 65536.0).roundToInt())
}
