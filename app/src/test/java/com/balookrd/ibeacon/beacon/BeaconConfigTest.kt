package com.balookrd.ibeacon.beacon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconConfigTest {

    @Test
    fun `parses dashed and bare UUIDs to the same value`() {
        val dashed = parseBeaconUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")
        val bare = parseBeaconUuid("E2C56DB5DFFB48D2B060D0F5A71096E0")

        assertNotNull(dashed)
        assertEquals(dashed, bare)
    }

    @Test
    fun `tolerates surrounding whitespace and lower case`() {
        assertNotNull(parseBeaconUuid("  e2c56db5-dffb-48d2-b060-d0f5a71096e0  "))
    }

    @Test
    fun `rejects malformed UUIDs`() {
        assertNull(parseBeaconUuid(""))
        assertNull(parseBeaconUuid("not-a-uuid"))
        assertNull(parseBeaconUuid("E2C56DB5DFFB48D2B060D0F5A71096"))
        assertNull(parseBeaconUuid("ZZC56DB5-DFFB-48D2-B060-D0F5A71096E0"))
        // Short groups that UUID.fromString would otherwise pad silently.
        assertNull(parseBeaconUuid("1-2-3-4-5"))
    }

    @Test
    fun `default config is valid`() {
        assertTrue(BeaconConfig().isValid())
    }

    @Test
    fun `reports every problem it finds`() {
        val problems = BeaconConfig(
            uuid = "nope",
            major = -1,
            minor = 65536,
            measuredPower = 5,
        ).validate()

        assertEquals(
            setOf(
                ConfigProblem.INVALID_UUID,
                ConfigProblem.MAJOR_OUT_OF_RANGE,
                ConfigProblem.MINOR_OUT_OF_RANGE,
                ConfigProblem.MEASURED_POWER_OUT_OF_RANGE,
            ),
            problems,
        )
    }

    @Test
    fun `accepts the boundary values`() {
        assertTrue(BeaconConfig(major = 0, minor = 65535, measuredPower = -127).isValid())
        assertTrue(BeaconConfig(major = 65535, minor = 0, measuredPower = 0).isValid())
    }
}
