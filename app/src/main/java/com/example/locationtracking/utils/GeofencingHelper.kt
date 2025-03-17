package com.example.locationtracking.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.locationtracking.broadcastReceiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class GeofencingHelper(private val context: Context) {

    // Tag for logging
    private val TAG = "GeofencingHelper"

    // Client for interacting with the geofencing APIs
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    // Pending intent that is triggered when a geofence transition occurs
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = "GEOFENCE_EVENT"
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Adds a list of geofences to be monitored.
     * @param geofenceList List of geofences to add
     * @param successListener Called when geofences are successfully added
     * @param failureListener Called when adding geofences fails
     */
    fun addGeofences(
        geofenceList: List<Geofence>,
        successListener: OnSuccessListener<Void>,
        failureListener: OnFailureListener
    ) {
        try {
            val geofencingRequest = buildGeofencingRequest(geofenceList)

            // Log that we're attempting to add geofences
            Log.d(TAG, "Adding ${geofenceList.size} geofences")

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when adding geofences: ${e.message}")
            failureListener.onFailure(e)
        }
    }

    /**
     * Removes all active geofences.
     * @param successListener Called when geofences are successfully removed
     * @param failureListener Called when removing geofences fails
     */
    fun removeGeofences(
        successListener: OnSuccessListener<Void>,
        failureListener: OnFailureListener
    ) {
        try {
            Log.d(TAG, "Removing all geofences")

            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when removing geofences: ${e.message}")
            failureListener.onFailure(e)
        }
    }

    /**
     * Creates a sample geofence at the specified location.
     * @param key Unique identifier for the geofence
     * @param latLng Center point of the geofence
     * @param radius Radius of the geofence in meters
     * @param transitionTypes Types of transitions to monitor (enter/exit/dwell)
     * @return A Geofence object
     */
    fun createGeofence(
        key: String,
        latLng: LatLng,
        radius: Float,
        transitionTypes: Int = Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
    ): Geofence {
        Log.d(TAG, "Creating geofence: $key at ${latLng.latitude}, ${latLng.longitude} with radius $radius meters")

        return Geofence.Builder()
            .setRequestId(key)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .build()
    }

    /**
     * Builds a GeofencingRequest with the provided list of geofences.
     * @param geofenceList List of geofences to include in the request
     * @return A GeofencingRequest object configured with the provided geofences
     */
    private fun buildGeofencingRequest(geofenceList: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "com.example.locationtracker.ACTION_GEOFENCE_EVENT"

        // Event types that can be broadcast
        const val GEOFENCE_ENTERED = "GEOFENCE_ENTERED"
        const val GEOFENCE_EXITED = "GEOFENCE_EXITED"
        const val GEOFENCE_UPDATED = "GEOFENCE_UPDATED"
    }
}