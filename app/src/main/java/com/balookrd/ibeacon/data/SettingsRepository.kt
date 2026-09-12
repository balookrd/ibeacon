package com.balookrd.ibeacon.data

import android.content.Context
import com.balookrd.ibeacon.beacon.AdvertiseRate
import com.balookrd.ibeacon.beacon.BeaconConfig
import com.balookrd.ibeacon.beacon.TxPower
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings live in device-protected storage so boot receivers can read them
 * before the user unlocks the phone — credential-protected storage (and thus
 * DataStore's default location) is unreadable at `LOCKED_BOOT_COMPLETED` time.
 *
 * [shouldRun] is the single source of truth for "the user wants the beacon on".
 * Every restart path consults it, so nothing revives a beacon stopped on purpose.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<BeaconConfig> = _config.asStateFlow()

    private val _shouldRun = MutableStateFlow(prefs.getBoolean(KEY_SHOULD_RUN, false))
    val shouldRun: StateFlow<Boolean> = _shouldRun.asStateFlow()

    fun saveConfig(config: BeaconConfig) {
        prefs.edit()
            .putString(KEY_UUID, config.uuid)
            .putInt(KEY_MAJOR, config.major)
            .putInt(KEY_MINOR, config.minor)
            .putInt(KEY_MEASURED_POWER, config.measuredPower)
            .putString(KEY_RATE, config.rate.name)
            .putString(KEY_TX_POWER, config.txPower.name)
            .apply()
        _config.value = config
    }

    /**
     * Written with `commit()`: a process killed a millisecond later must still
     * come back with the right intent.
     */
    fun setShouldRun(value: Boolean) {
        prefs.edit().putBoolean(KEY_SHOULD_RUN, value).commit()
        _shouldRun.value = value
    }

    private fun readConfig(): BeaconConfig {
        val defaults = BeaconConfig()
        return BeaconConfig(
            uuid = prefs.getString(KEY_UUID, defaults.uuid) ?: defaults.uuid,
            major = prefs.getInt(KEY_MAJOR, defaults.major),
            minor = prefs.getInt(KEY_MINOR, defaults.minor),
            measuredPower = prefs.getInt(KEY_MEASURED_POWER, defaults.measuredPower),
            rate = prefs.getString(KEY_RATE, null).toRateOr(defaults.rate),
            txPower = prefs.getString(KEY_TX_POWER, null).toTxPowerOr(defaults.txPower),
        )
    }

    companion object {
        private const val PREFS_NAME = "beacon_settings"
        private const val KEY_UUID = "uuid"
        private const val KEY_MAJOR = "major"
        private const val KEY_MINOR = "minor"
        private const val KEY_MEASURED_POWER = "measured_power"
        private const val KEY_RATE = "rate"
        private const val KEY_TX_POWER = "tx_power"
        private const val KEY_SHOULD_RUN = "should_run"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}

private fun String?.toRateOr(fallback: AdvertiseRate): AdvertiseRate =
    AdvertiseRate.entries.firstOrNull { it.name == this } ?: fallback

private fun String?.toTxPowerOr(fallback: TxPower): TxPower =
    TxPower.entries.firstOrNull { it.name == this } ?: fallback
