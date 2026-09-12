package com.balookrd.ibeacon.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.UserManagerCompat
import com.balookrd.ibeacon.data.SettingsRepository
import com.balookrd.ibeacon.service.BeaconService

/**
 * Brings the beacon back after a reboot or an app update.
 *
 * The receiver is direct-boot aware, so it also runs before the user unlocks the
 * device — that is why settings live in device-protected storage. WorkManager is
 * not available that early, so it is only scheduled once the user is unlocked.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = SettingsRepository.get(context)
        if (!repository.shouldRun.value) return

        Log.i(TAG, "restoring beacon after ${intent.action}")
        BeaconService.ensure(context)
        WatchdogAlarm.schedule(context, delayMs = FIRST_CHECK_DELAY_MS)
        if (UserManagerCompat.isUserUnlocked(context)) {
            WatchdogWorker.schedule(context)
        }
    }

    private companion object {
        private const val TAG = "BootReceiver"
        private const val FIRST_CHECK_DELAY_MS = 30_000L
    }
}
