package com.balookrd.ibeacon.beacon

import java.util.Locale
import java.util.UUID

/**
 * How often the radio puts an advertisement on the air. Android does not accept
 * an interval in milliseconds — only these three presets.
 */
enum class AdvertiseRate(val approxIntervalMs: Int) {
    LOW_POWER(1000),
    BALANCED(250),
    LOW_LATENCY(100),
}

/** Transmit power — controls range, not rate. */
enum class TxPower {
    ULTRA_LOW,
    LOW,
    MEDIUM,
    HIGH,
}

data class BeaconConfig(
    val uuid: String = DEFAULT_UUID,
    val major: Int = 1,
    val minor: Int = 1,
    /** RSSI measured at 1 m; receivers use it for distance estimation. */
    val measuredPower: Int = DEFAULT_MEASURED_POWER,
    val rate: AdvertiseRate = AdvertiseRate.BALANCED,
    val txPower: TxPower = TxPower.HIGH,
) {
    companion object {
        const val DEFAULT_UUID = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"
        const val DEFAULT_MEASURED_POWER = -59
        const val ID_MIN = 0
        const val ID_MAX = 65535
        const val MEASURED_POWER_MIN = -127
        const val MEASURED_POWER_MAX = 0
    }
}

enum class ConfigProblem {
    INVALID_UUID,
    MAJOR_OUT_OF_RANGE,
    MINOR_OUT_OF_RANGE,
    MEASURED_POWER_OUT_OF_RANGE,
}

/**
 * Accepts both the canonical dashed form and 32 bare hex digits, so a UUID
 * copied from a beacon vendor's console works either way.
 */
fun parseBeaconUuid(text: String): UUID? {
    val trimmed = text.trim()
    val canonical = when {
        trimmed.length == 32 && trimmed.all { it.isHexDigit() } -> buildString {
            append(trimmed, 0, 8); append('-')
            append(trimmed, 8, 12); append('-')
            append(trimmed, 12, 16); append('-')
            append(trimmed, 16, 20); append('-')
            append(trimmed, 20, 32)
        }

        trimmed.length == 36 -> trimmed
        else -> return null
    }
    return try {
        val parsed = UUID.fromString(canonical)
        // UUID.fromString is lenient about short groups; round-trip to reject those.
        if (parsed.toString().equals(canonical, ignoreCase = true)) parsed else null
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun BeaconConfig.validate(): Set<ConfigProblem> = buildSet {
    if (parseBeaconUuid(uuid) == null) add(ConfigProblem.INVALID_UUID)
    if (major !in BeaconConfig.ID_MIN..BeaconConfig.ID_MAX) add(ConfigProblem.MAJOR_OUT_OF_RANGE)
    if (minor !in BeaconConfig.ID_MIN..BeaconConfig.ID_MAX) add(ConfigProblem.MINOR_OUT_OF_RANGE)
    if (measuredPower !in BeaconConfig.MEASURED_POWER_MIN..BeaconConfig.MEASURED_POWER_MAX) {
        add(ConfigProblem.MEASURED_POWER_OUT_OF_RANGE)
    }
}

fun BeaconConfig.isValid(): Boolean = validate().isEmpty()

fun BeaconConfig.shortId(): String {
    val u = parseBeaconUuid(uuid)?.toString()?.uppercase(Locale.US) ?: uuid
    return "${u.takeLast(12)} · $major/$minor"
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
