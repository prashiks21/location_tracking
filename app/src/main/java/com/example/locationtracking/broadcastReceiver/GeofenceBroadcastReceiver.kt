package com.example.locationtracking.broadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.locationtracking.R
import com.example.locationtracking.utils.GeofencingHelper
import com.example.locationtracking.workManager.GeofenceStatusManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"
    private val NOTIFICATION_ID = 2001

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "GEOFENCE_EVENT") {
            Log.d(TAG, "Received action: ${intent.action} - Not a geofence event")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null. Intent Extras: ${intent.extras}")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage")

            // Broadcast the error
            val errorIntent = Intent("GEOFENCE_ERROR")
            errorIntent.putExtra("error_message", errorMessage)
            context.sendBroadcast(errorIntent)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test if the transition type is of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {


            val triggeringGeofences = geofencingEvent.triggeringGeofences

            Log.d(TAG, "Number of triggering geofences: ${triggeringGeofences?.size ?: 0}")

            // Create notification and broadcast based on transition type
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {

                    val geofenceIds = triggeringGeofences?.joinToString { it.requestId } ?: "unknown"
                    Log.d(TAG, "Entered geofence(s): $geofenceIds")


                    showNotification(context, "Entered geofence area", "You've entered a monitored area: $geofenceIds")

                    // Send a local broadcast that can be received by activities
                    val geofenceIntent = Intent("GEOFENCE_EVENT")
                    geofenceIntent.putExtra("transition_type", GeofencingHelper.GEOFENCE_ENTERED)
                    geofenceIntent.putExtra("geofence_ids", geofenceIds)
                    context.sendBroadcast(geofenceIntent)

                    // Update the GeofenceStatusManager
                    GeofenceStatusManager.updateStatus(GeofencingHelper.GEOFENCE_ENTERED, geofenceIds)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    // User exited the geofence
                    val geofenceIds = triggeringGeofences?.joinToString { it.requestId } ?: "unknown"
                    Log.d(TAG, "Exited geofence(s): $geofenceIds")

                    // Show notification
                    showNotification(context, "Left geofence area", "You've left a monitored area: $geofenceIds")

                    // Send a local broadcast that can be received by activities
                    val geofenceIntent = Intent("GEOFENCE_EVENT")
                    geofenceIntent.putExtra("transition_type", GeofencingHelper.GEOFENCE_EXITED)
                    geofenceIntent.putExtra("geofence_ids", geofenceIds)
                    context.sendBroadcast(geofenceIntent)

                    // Update the GeofenceStatusManager
                    GeofenceStatusManager.updateStatus(GeofencingHelper.GEOFENCE_EXITED, geofenceIds)
                }
            }
        } else {
            Log.w(TAG, "Geofence transition not of interest: $geofenceTransition")

            val geofenceIntent = Intent("GEOFENCE_EVENT")
            geofenceIntent.putExtra("transition_type", "OTHER")
            geofenceIntent.putExtra("transition_value", geofenceTransition)
            context.sendBroadcast(geofenceIntent)
        }
    }

    /**
     * Shows a notification when a geofence event occurs
     */
    private fun showNotification(context: Context, title: String, content: String) {
        val notificationBuilder = NotificationCompat.Builder(context, "geofence_channel")
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission: ${e.message}")
        }
    }
}
