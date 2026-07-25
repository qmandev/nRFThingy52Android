package com.armstrongmobile.nrfthingy52android.domain

// Little-endian read/write helpers shared by the Thingy wire-format codecs (plan §5). Pure
// Kotlin/JDK, no Android dependencies. Callers are responsible for bounds-checking before reading.

internal fun readInt32Le(data: ByteArray, offset: Int): Int =
    (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        ((data[offset + 2].toInt() and 0xFF) shl 16) or
        ((data[offset + 3].toInt() and 0xFF) shl 24)

internal fun readUInt32Le(data: ByteArray, offset: Int): Long =
    (data[offset].toLong() and 0xFF) or
        ((data[offset + 1].toLong() and 0xFF) shl 8) or
        ((data[offset + 2].toLong() and 0xFF) shl 16) or
        ((data[offset + 3].toLong() and 0xFF) shl 24)

internal fun int32Le(value: Int): ByteArray = byteArrayOf(
    value.toByte(),
    (value shr 8).toByte(),
    (value shr 16).toByte(),
    (value shr 24).toByte(),
)

internal fun uint32Le(value: Long): ByteArray = byteArrayOf(
    value.toByte(),
    (value shr 8).toByte(),
    (value shr 16).toByte(),
    (value shr 24).toByte(),
)

internal fun uint16Le(value: Int): ByteArray = byteArrayOf(
    value.toByte(),
    (value shr 8).toByte(),
)
