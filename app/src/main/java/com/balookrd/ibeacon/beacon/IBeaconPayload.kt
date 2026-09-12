package com.balookrd.ibeacon.beacon

import java.nio.ByteBuffer
import java.util.UUID

/**
 * iBeacon rides inside a manufacturer-specific AD structure owned by Apple.
 *
 * Layout of the bytes that follow the company ID:
 *
 *     02 15 | uuid[16] | major[2] | minor[2] | measuredPower[1]
 *
 * 0x02 marks the iBeacon sub-type, 0x15 is the length of everything after it
 * (21 bytes). All multi-byte fields are big-endian; measured power is signed.
 */
object IBeaconPayload {

    /** Apple's Bluetooth SIG company identifier. */
    const val APPLE_COMPANY_ID = 0x004C

    const val TYPE_PROXIMITY = 0x02.toByte()
    const val DATA_LENGTH = 0x15.toByte()

    /** 2 header bytes + 16 UUID + 2 major + 2 minor + 1 measured power. */
    const val PAYLOAD_SIZE = 23

    fun build(uuid: UUID, major: Int, minor: Int, measuredPower: Int): ByteArray {
        require(major in BeaconConfig.ID_MIN..BeaconConfig.ID_MAX) { "major out of range: $major" }
        require(minor in BeaconConfig.ID_MIN..BeaconConfig.ID_MAX) { "minor out of range: $minor" }
        require(measuredPower in -128..127) { "measured power out of range: $measuredPower" }

        return ByteBuffer.allocate(PAYLOAD_SIZE).apply {
            put(TYPE_PROXIMITY)
            put(DATA_LENGTH)
            putLong(uuid.mostSignificantBits)
            putLong(uuid.leastSignificantBits)
            putShort(major.toShort())
            putShort(minor.toShort())
            put(measuredPower.toByte())
        }.array()
    }

    /** Returns null when the config cannot be turned into a valid packet. */
    fun build(config: BeaconConfig): ByteArray? {
        val uuid = parseBeaconUuid(config.uuid) ?: return null
        if (!config.isValid()) return null
        return build(uuid, config.major, config.minor, config.measuredPower)
    }
}
