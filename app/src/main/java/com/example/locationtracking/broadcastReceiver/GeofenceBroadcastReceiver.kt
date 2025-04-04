package com.example.locationtracking.broadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.locationtracking.R
import com.example.locationtracking.utils.GeofencingHelper
import com.example.locationtracking.workManager.GeofenceStatusManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceList = geofencingEvent.triggeringGeofences
        for (geofence in geofenceList!!) {
            Log.d(TAG, "onReceive: " + geofence.requestId)
        }

        // Get the transition type.
        val transitionType = geofencingEvent.geofenceTransition
        Log.d(TAG, "onReceive: $transitionType")

        // Get the geofences that were triggered. A single event can trigger
        // multiple geofences.
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        // Get the transition details as a String.
//        val geofenceTransitionDetails = getGeofenceTransitionDetails(
//            this,
//            geofenceTransition,
//            triggeringGeofences
//        )
//
//        // Send notification and log the transition details.
//        sendNotification(geofenceTransitionDetails)

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "onReceive: Enter")
                Toast.makeText(context.applicationContext, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show()
            }

            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d(TAG, "onReceive: DWELL")
                Toast.makeText(context.applicationContext, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show()
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "onReceive: EXIT")
                Toast.makeText(context.applicationContext, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }
}
