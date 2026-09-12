package com.balookrd.ibeacon.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.balookrd.ibeacon.data.SettingsRepository
import com.balookrd.ibeacon.service.BeaconService

class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = SettingsRepository.get(context)
        if (!repository.shouldRun.value) {
            // The user stopped the beacon on purpose: let the chain die here.
            return
        }

        if (!BeaconService.isRunning) {
            Log.i(TAG, "service is down, restarting")
            BeaconService.ensure(context)
        }

        // Always re-arm: this is the only thing keeping the chain alive.
        WatchdogAlarm.schedule(context)
    }

    private companion object {
        private const val TAG = "WatchdogReceiver"
    }
}
