package com.armstrongmobile.nrfthingy52android.domain

import java.util.UUID

// Thingy:52 User Interface service (EF680300-…): the LED and Button characteristics (plan §5.1).
// Both are a single byte — 0x00 off/released, 0x01 on/pressed. Ported from the iOS
// ThingyPeripheral.swift, which holds these UUIDs and the same one-byte convention.
object ThingyUserInterface {
    val serviceUuid: UUID = UUID.fromString("EF680300-9B35-4933-9B10-52FFA9740042")
    val ledCharacteristicUuid: UUID = UUID.fromString("EF680301-9B35-4933-9B10-52FFA9740042")
    val buttonCharacteristicUuid: UUID = UUID.fromString("EF680302-9B35-4933-9B10-52FFA9740042")

    // True when the LED is on. Null for an empty payload.
    fun parseLed(data: ByteArray): Boolean? = data.firstOrNull()?.let { it.toInt() != 0x00 }

    // True when the button is pressed. Null for an empty payload.
    fun parseButton(data: ByteArray): Boolean? = data.firstOrNull()?.let { it.toInt() != 0x00 }

    fun encodeLed(on: Boolean): ByteArray = byteArrayOf(if (on) 0x01 else 0x00)
}
