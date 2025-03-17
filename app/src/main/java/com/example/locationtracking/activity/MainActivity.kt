package com.example.locationtracking.activity


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.locationtracking.databinding.ActivityMainBinding
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.locationtracking.workManager.LocationTrackingServiceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionsHandler: PermissionsHandler

    // BroadcastReceiver for geofence events
    private val geofenceReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "GEOFENCE_EVENT") {
                val transitionType = intent.getStringExtra("transition_type")
                Log.d("GeofenceReceiver", "Transition type: $transitionType")
                updateGeofenceStatus(transitionType)
            }
        }
    }

    // Permission request launcher
   /* private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            // All permissions granted
            startLocationTracking()
        } else {
            // Some permissions denied
            Toast.makeText(this, "Location permissions are required", Toast.LENGTH_LONG).show()
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionsHandler = com.example.locationtracking.activity.PermissionsHandler(this)

        binding.startButton.setOnClickListener {
            requestPermissionsAndStartTracking()
        }

        binding.stopButton.setOnClickListener {
            stopLocationTracking()
        }

        // Register the broadcast receiver for geofence events
        ContextCompat.registerReceiver(
            this,
            geofenceReceiver,
            IntentFilter("GEOFENCE_EVENT"),
            ContextCompat.RECEIVER_EXPORTED
        )

        // Observe location updates using LiveData
        LocationTrackingServiceManager.locationLiveData.observe(this) { location ->
            location?.let {
                updateLocationUI(it.latitude, it.longitude)
            }
        }

        // Or collect location flow updates
        lifecycleScope.launch {
            LocationTrackingServiceManager.locationFlow.collect { location ->
                location?.let {
                    updateLocationUI(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun requestPermissionsAndStartTracking() {
        permissionsHandler.requestLocationPermissions { allGranted ->
            if (allGranted) {
                // All necessary permissions granted, start location tracking
                startLocationTracking()
            } else {
                // Some permissions were denied
                Toast.makeText(
                    this,
                    "Location tracking requires the requested permissions",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver
        unregisterReceiver(geofenceReceiver)
    }

    /*private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Add foreground service permission for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        // Add background location permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Check if we already have the permissions
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            startLocationTracking()
        } else {
            // Request permissions
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }*/

    private fun startLocationTracking() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java).apply {
            putExtra("setup_geofence", true)
        }

        // Start the foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationTracking() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)

        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateLocationUI(latitude: Double, longitude: Double) {
        binding.locationTextView.text = "Latitude: $latitude\nLongitude: $longitude"
    }

    private fun updateGeofenceStatus(transitionType: String?) {
        when (transitionType) {
            "ENTER" -> binding.geofenceStatusTextView.text = "Geofence Status: Entered target area"
            "EXIT" -> binding.geofenceStatusTextView.text = "Geofence Status: Exited target area"
            else -> binding.geofenceStatusTextView.text = "Geofence Status: Unknown"
        }
    }
}
