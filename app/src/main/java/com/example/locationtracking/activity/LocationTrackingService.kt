package com.example.locationtracking.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.locationtracking.R
import com.example.locationtracking.broadcastReceiver.GeofenceBroadcastReceiver
import com.example.locationtracking.utils.GeofencingHelper
import com.example.locationtracking.viewModel.LocationViewModel
import com.example.locationtracking.workManager.LocationTrackingServiceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTrackingService : Service() {

    private val TAG = "LocationService"
    // Coroutine scope for service operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // FusedLocationProviderClient for location updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Location request configuration
    private lateinit var locationRequest: LocationRequest

    // Location callback for handling location updates
    private lateinit var locationCallback: LocationCallback

    // Notification ID for foreground service
    private val NOTIFICATION_ID = 1001

    // Notification channel ID
    private val CHANNEL_ID = "location_tracking_channel"

    // Flow for reactive location updates
    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow

    // LiveData for reactive location updates (alternative to Flow)
    private val _locationLiveData = MutableLiveData<Location?>()
    val locationLiveData: LiveData<Location?> = _locationLiveData

    // Geofencing client
    private lateinit var geofencingClient: GeofencingClient
    // Geofencing components
    private lateinit var geofencingHelper: GeofencingHelper
    private var isGeofencingActive = false

    override fun onCreate() {
        super.onCreate()

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the GeofencingClient
        geofencingClient = LocationServices.getGeofencingClient(this)

        // Create notification channel for Android O and above
        createNotificationChannel()

        // Configure location request settings
        createLocationRequest()

        // Setup location callback
        setupLocationCallback()

        geofencingHelper = GeofencingHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the service as a foreground service with a notification
        startForeground()

        // Start requesting location updates
        startLocationUpdates()

        // If included in the intent, setup geofencing
        intent?.let {
            if (it.hasExtra("setup_geofence")) {
                setupGeofencing()
            }
        }

        // If service is killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates
        stopLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Location Tracking"
            val descriptionText = "Tracks your location in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Tracking your location in the background")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000) // 15 seconds
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(10000) // 10 seconds
            .setMaxUpdateDelayMillis(30000) // 30 seconds
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
//                    // Update Flow
//                    _locationFlow.value = location
//
//                    // Update LiveData
//                    _locationLiveData.postValue(location)

                    LocationTrackingServiceManager.updateLocation(location)
                    updateNotification(location)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateNotification(location: Location) {
        // Create updated notification content with current location
        val notificationText = "Lat: ${String.format("%.4f", location.latitude)}, Long: ${
            String.format(
                "%.4f",
                location.longitude
            )
        }"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification(notificationText)

        // Update the foreground service notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(contentText: String = "Tracking your location in the background"): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {

        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setupGeofencing() {
        Log.d(TAG, "setupGeofencing: ")

        val geofence = Geofence.Builder()
            .setRequestId("my_geofence")
            .setCircularRegion(
                21.1952, // latitude
                79.1104, // longitude
                500f // radius in meters
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        // Create GeofencingRequest
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Create the PendingIntent for the geofence trigger
        val geofencePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Register the geofence
        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("GeofenceSetup", "Geofence added successfully")
                }.addOnFailureListener { e ->
                    Log.e("GeofenceSetup", "Failed to add geofence", e)
                }
        } catch (e: SecurityException) {

        }
    }
}
