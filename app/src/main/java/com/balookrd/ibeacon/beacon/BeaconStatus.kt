package com.balookrd.ibeacon.beacon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface BeaconFailure {
    /** Device has no Bluetooth adapter at all. */
    data object BleUnsupported : BeaconFailure

    /** Adapter exists but cannot act as a peripheral (no advertiser). */
    data object AdvertisingUnsupported : BeaconFailure

    data object BluetoothOff : BeaconFailure

    data object PermissionMissing : BeaconFailure

    data object InvalidConfig : BeaconFailure

    /** `AdvertiseCallback.onStartFailure` code, or an unexpected exception. */
    data class SystemError(val code: Int) : BeaconFailure
}

data class BeaconStatus(
    val serviceRunning: Boolean = false,
    val advertising: Boolean = false,
    val config: BeaconConfig? = null,
    val failure: BeaconFailure? = null,
    /** `SystemClock.elapsedRealtime()` of the last successful start. */
    val advertisingSinceElapsedMs: Long? = null,
    /** How many times a watchdog or restart path had to revive the beacon. */
    val revivals: Int = 0,
)

/**
 * Single place the service publishes to and the UI observes. Survives the
 * activity but not the process — that is the correct lifetime here, since a
 * dead process means the beacon is down anyway.
 */
object BeaconStatusBus {
    private val _state = MutableStateFlow(BeaconStatus())
    val state: StateFlow<BeaconStatus> = _state.asStateFlow()

    fun update(transform: (BeaconStatus) -> BeaconStatus) = _state.update(transform)

    fun countRevival() = _state.update { it.copy(revivals = it.revivals + 1) }
}
