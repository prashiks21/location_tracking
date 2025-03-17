package com.example.locationtracking.workManager

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.locationtracking.activity.LocationTrackingService
import java.util.concurrent.TimeUnit

class LocationWorkManager(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        // Start the location tracking service
        val serviceIntent = Intent(applicationContext, LocationTrackingService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }

        return Result.success()
    }

    companion object {
        // Schedule periodic work with constraints
        fun schedulePeriodicWork(context: Context) {
            // Define constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Only when connected to network
                .setRequiresBatteryNotLow(true) // Not when battery is low
                .build()

            // Create periodic work request
            val workRequest = PeriodicWorkRequestBuilder<LocationWorkManager>(
                15, TimeUnit.MINUTES, // Minimum interval of 15 minutes
                5, TimeUnit.MINUTES // Flex interval of 5 minutes
            )
                .setConstraints(constraints)
                .build()

            // Enqueue the work request
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "location_tracking_work",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }

        // Cancel scheduled work
        fun cancelWork(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("location_tracking_work")
        }
    }
}