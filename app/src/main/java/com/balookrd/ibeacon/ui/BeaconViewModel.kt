package com.balookrd.ibeacon.ui

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.balookrd.ibeacon.beacon.AdvertiseRate
import com.balookrd.ibeacon.beacon.BeaconAdvertiser
import com.balookrd.ibeacon.beacon.BeaconConfig
import com.balookrd.ibeacon.beacon.BeaconStatusBus
import com.balookrd.ibeacon.beacon.ConfigProblem
import com.balookrd.ibeacon.beacon.TxPower
import com.balookrd.ibeacon.beacon.validate
import com.balookrd.ibeacon.data.SettingsRepository
import com.balookrd.ibeacon.keepalive.KeepAliveHelper
import com.balookrd.ibeacon.service.BeaconService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/** Text-level mirror of [BeaconConfig] — the UI edits strings, not ints. */
data class FormState(
    val uuid: String,
    val major: String,
    val minor: String,
    val measuredPower: String,
    val rate: AdvertiseRate,
    val txPower: TxPower,
) {
    fun toConfig(): BeaconConfig = BeaconConfig(
        uuid = uuid.trim(),
        // Out-of-range sentinels keep validation in one place instead of
        // duplicating "is this even a number" checks in the UI.
        major = major.trim().toIntOrNull() ?: -1,
        minor = minor.trim().toIntOrNull() ?: -1,
        measuredPower = measuredPower.trim().toIntOrNull() ?: Int.MIN_VALUE,
        rate = rate,
        txPower = txPower,
    )

    val problems: Set<ConfigProblem> get() = toConfig().validate()

    companion object {
        fun from(config: BeaconConfig) = FormState(
            uuid = config.uuid,
            major = config.major.toString(),
            minor = config.minor.toString(),
            measuredPower = config.measuredPower.toString(),
            rate = config.rate,
            txPower = config.txPower,
        )
    }
}

data class Checklist(
    val advertisePermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val batteryUnrestricted: Boolean,
    val exactAlarmsAllowed: Boolean,
    val bluetoothOn: Boolean,
    val advertisingSupported: Boolean,
    val vendorLabel: String?,
    val hasAutostartScreen: Boolean,
)

class BeaconViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SettingsRepository.get(app)

    private val _form = MutableStateFlow(FormState.from(repository.config.value))
    val form: StateFlow<FormState> = _form.asStateFlow()

    private val _checklist = MutableStateFlow(readChecklist())
    val checklist: StateFlow<Checklist> = _checklist.asStateFlow()

    val status = BeaconStatusBus.state
    val shouldRun: StateFlow<Boolean> = repository.shouldRun

    /** What the running beacon actually uses, as opposed to the edited form. */
    val savedConfig: StateFlow<BeaconConfig> = repository.config

    fun onUuidChange(value: String) = update { it.copy(uuid = value) }

    fun onMajorChange(value: String) = update { it.copy(major = value.filterDigits()) }

    fun onMinorChange(value: String) = update { it.copy(minor = value.filterDigits()) }

    fun onMeasuredPowerChange(value: String) = update { it.copy(measuredPower = value.filterSigned()) }

    fun onRateChange(rate: AdvertiseRate) = update { it.copy(rate = rate) }

    fun onTxPowerChange(txPower: TxPower) = update { it.copy(txPower = txPower) }

    fun randomizeUuid() = update { it.copy(uuid = UUID.randomUUID().toString().uppercase(Locale.US)) }

    /** Returns false when the form still has problems; the UI then shows them. */
    fun startOrApply(): Boolean {
        val config = _form.value.toConfig()
        if (config.validate().isNotEmpty()) return false
        repository.saveConfig(config)
        BeaconService.start(getApplication())
        return true
    }

    fun stop() = BeaconService.stop(getApplication())

    /**
     * Being in the foreground is the one context every OEM lets a service start
     * from. Aggressive shells (MagicOS, EMUI, MIUI) block the background paths —
     * boot receiver, package-replaced receiver, watchdog alarms — so opening the
     * app is the reliable fallback that puts the beacon back on the air.
     */
    fun resumeIfNeeded() {
        if (repository.shouldRun.value && !BeaconService.isRunning) {
            BeaconService.ensure(getApplication())
        }
    }

    fun refreshChecklist() {
        _checklist.value = readChecklist()
    }

    private fun update(transform: (FormState) -> FormState) {
        _form.value = transform(_form.value)
    }

    private fun readChecklist(): Checklist {
        val context = getApplication<Application>()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        return Checklist(
            advertisePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            batteryUnrestricted = KeepAliveHelper.isIgnoringBatteryOptimizations(context),
            exactAlarmsAllowed = KeepAliveHelper.canScheduleExactAlarms(context),
            bluetoothOn = adapter?.isEnabled == true,
            advertisingSupported = BeaconAdvertiser.isSupported(context),
            vendorLabel = KeepAliveHelper.vendorLabel(context),
            hasAutostartScreen = KeepAliveHelper.autostartIntent(context) != null,
        )
    }
}

private fun String.filterDigits() = filter { it.isDigit() }.take(5)

private fun String.filterSigned() = filterIndexed { index, c ->
    c.isDigit() || (c == '-' && index == 0)
}.take(4)
