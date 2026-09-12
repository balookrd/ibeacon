package com.balookrd.ibeacon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.balookrd.ibeacon.R
import com.balookrd.ibeacon.beacon.BeaconAdvertiser
import com.balookrd.ibeacon.beacon.BeaconConfig
import com.balookrd.ibeacon.beacon.BeaconFailure
import com.balookrd.ibeacon.beacon.BeaconStatusBus
import com.balookrd.ibeacon.beacon.describe
import com.balookrd.ibeacon.beacon.shortId
import com.balookrd.ibeacon.data.SettingsRepository
import com.balookrd.ibeacon.keepalive.WatchdogAlarm
import com.balookrd.ibeacon.keepalive.WatchdogWorker
import com.balookrd.ibeacon.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the advertisement.
 *
 * It is deliberately dumb: while [SettingsRepository.shouldRun] is set, it keeps
 * the beacon on the air and re-raises it whenever something knocks it down —
 * Bluetooth toggled off and back on, the stack dropping the advertisement, or
 * the whole process being killed and restarted by one of the watchdogs.
 */
class BeaconService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: SettingsRepository
    private lateinit var advertiser: BeaconAdvertiser
    private var wakeLock: PowerManager.WakeLock? = null
    private var selfCheckJob: Job? = null

    /** The config currently on the air, or null when nothing is being advertised. */
    private var appliedConfig: BeaconConfig? = null

    /** Re-raises the beacon when the user turns Bluetooth back on. */
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    Log.i(TAG, "bluetooth back on, restarting advertisement")
                    restartAdvertising(countAsRevival = true)
                }

                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                    advertiser.stop()
                    publish(advertising = false, failure = BeaconFailure.BluetoothOff)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        repository = SettingsRepository.get(this)
        advertiser = BeaconAdvertiser(this) { failure -> publish(advertising = false, failure = failure) }
        createNotificationChannel()
        ContextCompat.registerReceiver(
            this,
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must happen within a few seconds of the start request, before anything else.
        goForeground()

        if (intent?.action == ACTION_STOP) {
            stopBeacon()
            return START_NOT_STICKY
        }

        repository.setShouldRun(true)
        ensureAdvertising(revivalRequest = intent?.action == ACTION_ENSURE)
        WatchdogAlarm.schedule(this)
        WatchdogWorker.schedule(this)
        startSelfCheck()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Swiping the app out of recents tears the service down on many OEM builds.
     * Schedule a near-immediate alarm so it comes straight back.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (repository.shouldRun.value) {
            WatchdogAlarm.schedule(this, delayMs = TASK_REMOVED_RESTART_DELAY_MS)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        selfCheckJob = null
        scope.cancel()
        advertiser.stop()
        appliedConfig = null
        runCatching { unregisterReceiver(bluetoothStateReceiver) }
        releaseWakeLock()
        BeaconStatusBus.update { it.copy(serviceRunning = false, advertising = false) }
        // If the user never asked us to stop, something else killed us: come back.
        if (repository.shouldRun.value) {
            WatchdogAlarm.schedule(this, delayMs = DESTROY_RESTART_DELAY_MS)
        }
        super.onDestroy()
    }

    /**
     * Start requests arrive in bursts — `LOCKED_BOOT_COMPLETED` and
     * `BOOT_COMPLETED` land back to back, and both watchdogs can fire at once.
     * Re-raising an advertisement that is already correct would take the beacon
     * off the air for a moment for nothing, so check first.
     */
    private fun ensureAdvertising(revivalRequest: Boolean) {
        if (advertiser.isAdvertising && appliedConfig == repository.config.value) {
            updateNotification()
            return
        }
        if (revivalRequest) {
            Log.i(TAG, "revived by a watchdog")
            BeaconStatusBus.countRevival()
        }
        restartAdvertising(countAsRevival = false)
    }

    private fun restartAdvertising(countAsRevival: Boolean) {
        val config = repository.config.value
        val failure = advertiser.start(config)
        appliedConfig = if (failure == null) config else null
        if (countAsRevival && failure == null) BeaconStatusBus.countRevival()
        publish(advertising = failure == null, failure = failure)
    }

    /**
     * The stack can drop an advertisement without ever calling back — a periodic
     * poll is the only reliable way to notice.
     */
    private fun startSelfCheck() {
        if (selfCheckJob?.isActive == true) return
        selfCheckJob = scope.launch {
            while (isActive) {
                delay(SELF_CHECK_INTERVAL_MS)
                if (!repository.shouldRun.value) continue
                if (!advertiser.isAdvertising) {
                    Log.w(TAG, "advertisement is down, re-raising")
                    restartAdvertising(countAsRevival = true)
                }
            }
        }
    }

    private fun stopBeacon() {
        advertiser.stop()
        appliedConfig = null
        repository.setShouldRun(false)
        WatchdogAlarm.cancel(this)
        WatchdogWorker.cancel(this)
        BeaconStatusBus.update {
            it.copy(serviceRunning = false, advertising = false, failure = null, advertisingSinceElapsedMs = null)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publish(advertising: Boolean, failure: BeaconFailure?) {
        BeaconStatusBus.update {
            it.copy(
                serviceRunning = true,
                advertising = advertising,
                config = repository.config.value,
                failure = failure,
                advertisingSinceElapsedMs = if (advertising) {
                    it.advertisingSinceElapsedMs ?: SystemClock.elapsedRealtime()
                } else {
                    null
                },
            )
        }
        updateNotification()
    }

    private fun goForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val status = BeaconStatusBus.state.value
        val config = repository.config.value

        // Collapsed, the shade fits only a line — show the tail of the UUID with a
        // leading ellipsis, so a changed prefix is never mistaken for a stale
        // notification. Expanded, show the identity in full.
        val text = when {
            status.failure != null -> status.failure.describe()
            status.advertising -> "…${config.shortId()} · ${config.rate.approxIntervalMs} мс"
            else -> "Запуск…"
        }
        val expandedText = when {
            status.failure != null -> status.failure.describe()
            status.advertising -> buildString {
                append(config.uuid).append('\n')
                append("major ${config.major} · minor ${config.minor} · ${config.measuredPower} dBm\n")
                append("интервал ~${config.rate.approxIntervalMs} мс")
            }

            else -> "Запуск…"
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_CONTENT,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, BeaconService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_beacon)
            .setContentTitle(if (status.advertising) "iBeacon вещает" else "iBeacon не вещает")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setContentIntent(contentIntent)
            .addAction(0, "Остановить", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Трансляция iBeacon",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Постоянное уведомление, пока приложение вещает iBeacon"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    companion object {
        private const val TAG = "BeaconService"
        private const val CHANNEL_ID = "beacon_broadcast"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_STOP = 2
        private const val WAKE_LOCK_TAG = "iBeacon:advertise"
        private const val SELF_CHECK_INTERVAL_MS = 30_000L
        private const val TASK_REMOVED_RESTART_DELAY_MS = 1_000L
        private const val DESTROY_RESTART_DELAY_MS = 2_000L

        const val ACTION_START = "com.balookrd.ibeacon.action.START"
        const val ACTION_STOP = "com.balookrd.ibeacon.action.STOP"
        const val ACTION_ENSURE = "com.balookrd.ibeacon.action.ENSURE"

        /**
         * Lives and dies with the process, which is exactly the question the
         * watchdogs ask: if the process is gone, so is the beacon.
         */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) = launch(context, ACTION_START)

        fun ensure(context: Context) = launch(context, ACTION_ENSURE)

        fun stop(context: Context) = launch(context, ACTION_STOP)

        private fun launch(context: Context, action: String) {
            val intent = Intent(context, BeaconService::class.java).setAction(action)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException on API 31+: we were
                // called from a background context the platform does not trust.
                // The alarm watchdog is an allowed context, so try again there.
                Log.w(TAG, "foreground start refused, deferring to watchdog", e)
                if (action != ACTION_STOP) WatchdogAlarm.schedule(context, delayMs = 1_000L)
            }
        }
    }
}
