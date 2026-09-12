package com.balookrd.ibeacon.beacon

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class IBeaconPayloadTest {

    @Test
    fun `builds the canonical 23-byte layout`() {
        val payload = IBeaconPayload.build(
            uuid = UUID.fromString("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"),
            major = 1,
            minor = 2,
            measuredPower = -59,
        )

        val expected = byteArrayOf(
            0x02, 0x15,
            0xE2.toByte(), 0xC5.toByte(), 0x6D, 0xB5.toByte(),
            0xDF.toByte(), 0xFB.toByte(),
            0x48, 0xD2.toByte(),
            0xB0.toByte(), 0x60,
            0xD0.toByte(), 0xF5.toByte(), 0xA7.toByte(), 0x10, 0x96.toByte(), 0xE0.toByte(),
            0x00, 0x01,
            0x00, 0x02,
            0xC5.toByte(),
        )
        assertArrayEquals(expected, payload)
        assertEquals(IBeaconPayload.PAYLOAD_SIZE, payload.size)
    }

    @Test
    fun `major and minor are big-endian`() {
        val payload = IBeaconPayload.build(UUID.randomUUID(), major = 1000, minor = 65535, measuredPower = -59)

        assertEquals(0x03.toByte(), payload[18])
        assertEquals(0xE8.toByte(), payload[19])
        assertEquals(0xFF.toByte(), payload[20])
        assertEquals(0xFF.toByte(), payload[21])
    }

    @Test
    fun `measured power is written as a signed byte`() {
        val payload = IBeaconPayload.build(UUID.randomUUID(), major = 0, minor = 0, measuredPower = -127)

        assertEquals((-127).toByte(), payload[22])
    }

    @Test
    fun `fits into a BLE advertisement`() {
        val payload = IBeaconPayload.build(UUID.randomUUID(), 1, 1, -59)

        // 3 bytes of flags + 2 bytes of AD header + 2 bytes of company ID + payload.
        val onAir = 3 + 2 + 2 + payload.size
        assert(onAir <= 31) { "advertisement would be $onAir bytes" }
    }

    @Test
    fun `accepts a UUID without dashes`() {
        val dashed = IBeaconPayload.build(BeaconConfig(uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"))
        val bare = IBeaconPayload.build(BeaconConfig(uuid = "E2C56DB5DFFB48D2B060D0F5A71096E0"))

        assertNotNull(dashed)
        assertArrayEquals(dashed, bare)
    }

    @Test
    fun `rejects an invalid config`() {
        assertNull(IBeaconPayload.build(BeaconConfig(uuid = "not-a-uuid")))
        assertNull(IBeaconPayload.build(BeaconConfig(major = 70000)))
        assertNull(IBeaconPayload.build(BeaconConfig(minor = -1)))
        assertNull(IBeaconPayload.build(BeaconConfig(measuredPower = -200)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an out-of-range major`() {
        IBeaconPayload.build(UUID.randomUUID(), major = 65536, minor = 0, measuredPower = -59)
    }
}
