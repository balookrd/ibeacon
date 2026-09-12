package com.balookrd.ibeacon.keepalive

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.balookrd.ibeacon.data.SettingsRepository
import com.balookrd.ibeacon.service.BeaconService
import java.util.concurrent.TimeUnit

/**
 * The slow half of the watchdog pair. WorkManager's schedule is persisted by the
 * framework and restored after reboot, so it covers the case where every alarm
 * of ours was wiped — at the cost of a 15-minute minimum period.
 */
class WatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = SettingsRepository.get(applicationContext)
        if (repository.shouldRun.value && !BeaconService.isRunning) {
            Log.i(TAG, "service is down, restarting")
            BeaconService.ensure(applicationContext)
        }
        // Alarms do not survive every OEM cleanup; re-arm from here as well.
        if (repository.shouldRun.value) {
            WatchdogAlarm.schedule(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "WatchdogWorker"
        private const val WORK_NAME = "beacon-watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }.onFailure {
                // WorkManager is unavailable before the user unlocks the device.
                Log.w(TAG, "could not schedule periodic watchdog", it)
            }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
        }
    }
}
