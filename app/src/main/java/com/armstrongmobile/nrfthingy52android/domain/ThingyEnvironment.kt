package com.armstrongmobile.nrfthingy52android.domain

import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

// Thingy:52 Environment service (EF680200-…): characteristic UUIDs plus pure parse/encode for each
// characteristic's wire format (plan §5.2). The encoders mirror the parsers and back the fake
// transport (Phase 3). Ported 1:1 from the iOS ThingyEnvironment.swift; no Android dependencies.
object ThingyEnvironment {
    val serviceUuid: UUID = UUID.fromString("EF680200-9B35-4933-9B10-52FFA9740042")
    val temperatureCharacteristicUuid: UUID = UUID.fromString("EF680201-9B35-4933-9B10-52FFA9740042")
    val pressureCharacteristicUuid: UUID = UUID.fromString("EF680202-9B35-4933-9B10-52FFA9740042")
    val humidityCharacteristicUuid: UUID = UUID.fromString("EF680203-9B35-4933-9B10-52FFA9740042")
    val airQualityCharacteristicUuid: UUID = UUID.fromString("EF680204-9B35-4933-9B10-52FFA9740042")

    // int8 integer part + uint8 hundredths → °C. Byte is signed, matching Swift Int8, so the sign of
    // the integer part carries through and the decimal part follows that sign.
    fun parseTemperature(data: ByteArray): EnvironmentReading? {
        if (data.size < 2) return null
        val integer = data[0].toInt()
        val decimal = data[1].toInt() and 0xFF
        val sign = if (integer < 0) -1.0 else 1.0
        return EnvironmentReading.Temperature(integer + sign * decimal / 100.0)
    }

    // int32 LE integer hPa + uint8 hundredths → hPa.
    fun parsePressure(data: ByteArray): EnvironmentReading? {
        if (data.size < 5) return null
        val integer = readInt32Le(data, 0)
        val decimal = data[4].toInt() and 0xFF
        return EnvironmentReading.Pressure(integer + decimal / 100.0)
    }

    // uint8 %RH.
    fun parseHumidity(data: ByteArray): EnvironmentReading? {
        if (data.isEmpty()) return null
        return EnvironmentReading.Humidity(data[0].toInt() and 0xFF)
    }

    // uint16 LE eCO2 ppm + uint16 LE TVOC ppb.
    fun parseAirQuality(data: ByteArray): EnvironmentReading? {
        if (data.size < 4) return null
        val eco2 = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        val tvoc = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
        return EnvironmentReading.AirQuality(eco2, tvoc)
    }

    fun encodeTemperature(celsius: Double): ByteArray {
        val integer = celsius.toInt().coerceIn(-128, 127) // toInt() truncates toward zero
        val decimal = ((abs(celsius) * 100).roundToInt() % 100).coerceIn(0, 255)
        return byteArrayOf(integer.toByte(), decimal.toByte())
    }

    fun encodePressure(hPa: Double): ByteArray {
        val integer = hPa.toInt()
        val decimal = ((hPa * 100).roundToInt() % 100).coerceIn(0, 255)
        return int32Le(integer) + byteArrayOf(decimal.toByte())
    }

    fun encodeHumidity(percent: Int): ByteArray = byteArrayOf(percent.coerceIn(0, 255).toByte())

    fun encodeAirQuality(eco2: Int, tvoc: Int): ByteArray = uint16Le(eco2 and 0xFFFF) + uint16Le(tvoc and 0xFFFF)
}
