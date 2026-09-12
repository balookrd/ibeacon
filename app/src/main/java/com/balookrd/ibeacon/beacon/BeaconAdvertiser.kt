package com.balookrd.ibeacon.beacon

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Thin wrapper around [BluetoothLeAdvertiser]: keeps exactly one advertisement
 * up and turns every way it can go wrong into a [BeaconFailure].
 */
class BeaconAdvertiser(
    private val context: Context,
    private val onFailure: (BeaconFailure) -> Unit,
) {

    private var advertiser: BluetoothLeAdvertiser? = null
    private var callback: AdvertiseCallback? = null

    val isAdvertising: Boolean
        get() = callback != null

    /** Returns null when the advertisement was handed to the stack. */
    @SuppressLint("MissingPermission")
    fun start(config: BeaconConfig): BeaconFailure? {
        stop()

        val payload = IBeaconPayload.build(config) ?: return BeaconFailure.InvalidConfig
        if (!hasAdvertisePermission()) return BeaconFailure.PermissionMissing

        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter ?: return BeaconFailure.BleUnsupported
        if (!adapter.isEnabled) return BeaconFailure.BluetoothOff

        val leAdvertiser = adapter.bluetoothLeAdvertiser ?: return BeaconFailure.AdvertisingUnsupported

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(config.rate.toAndroidMode())
            .setTxPowerLevel(config.txPower.toAndroidTxPower())
            .setConnectable(false)
            // 0 = advertise until stopped; anything else caps out at ~3 minutes.
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(IBeaconPayload.APPLE_COMPANY_ID, payload)
            .build()

        val newCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i(TAG, "advertising started, txPower=${settingsInEffect.txPowerLevel}")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.w(TAG, "advertising failed, code=$errorCode")
                callback = null
                advertiser = null
                onFailure(BeaconFailure.SystemError(errorCode))
            }
        }

        return try {
            leAdvertiser.startAdvertising(settings, data, newCallback)
            advertiser = leAdvertiser
            callback = newCallback
            null
        } catch (e: SecurityException) {
            Log.w(TAG, "startAdvertising denied", e)
            BeaconFailure.PermissionMissing
        } catch (e: IllegalStateException) {
            // Thrown when the adapter is torn down between the check and the call.
            Log.w(TAG, "startAdvertising rejected", e)
            BeaconFailure.BluetoothOff
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val current = callback ?: return
        try {
            advertiser?.stopAdvertising(current)
        } catch (e: SecurityException) {
            Log.w(TAG, "stopAdvertising denied", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "stopAdvertising rejected", e)
        }
        callback = null
        advertiser = null
    }

    private fun hasAdvertisePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            // BLUETOOTH_ADMIN is an install-time permission on these versions.
            true
        }

    companion object {
        private const val TAG = "BeaconAdvertiser"

        /** True when this device can act as a BLE peripheral at all. */
        fun isSupported(context: Context): Boolean {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return false
            return adapter.isMultipleAdvertisementSupported || adapter.bluetoothLeAdvertiser != null
        }
    }
}

private fun AdvertiseRate.toAndroidMode(): Int = when (this) {
    AdvertiseRate.LOW_POWER -> AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
    AdvertiseRate.BALANCED -> AdvertiseSettings.ADVERTISE_MODE_BALANCED
    AdvertiseRate.LOW_LATENCY -> AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
}

private fun TxPower.toAndroidTxPower(): Int = when (this) {
    TxPower.ULTRA_LOW -> AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW
    TxPower.LOW -> AdvertiseSettings.ADVERTISE_TX_POWER_LOW
    TxPower.MEDIUM -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
    TxPower.HIGH -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
}
