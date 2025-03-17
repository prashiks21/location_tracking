package com.example.locationtracking.activity

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class PermissionsHandler(private val activity: FragmentActivity) {

    // Permission request launchers for different permission types
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    // Callback for when all permissions are granted or denied
    private var onPermissionsResult: ((Boolean) -> Unit)? = null

    // Flag to track if we need to request background location separately
    private var needsBackgroundLocationRequest = false

    // Initialize permission launchers
    init {
        setupPermissionLaunchers()
    }

    /**
     * Sets up the permission request launchers
     */
    private fun setupPermissionLaunchers() {

        locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                // If we need background location and on Android 10+, request it separately
                if (needsBackgroundLocationRequest && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocationPermission()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasNotificationPermission()) {
                    // If we need notification permission for Android 13+
                    requestNotificationPermission()
                } else {
                    // All permissions granted
                    onPermissionsResult?.invoke(true)
                }
            } else {
                // Some permissions were denied
                onPermissionsResult?.invoke(false)
            }
        }

        // Special launcher just for background location (Android 10+)
        backgroundLocationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Check if we need notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasNotificationPermission()) {
                    requestNotificationPermission()
                } else {
                    onPermissionsResult?.invoke(true)
                }
            } else {
                // Background location permission denied
                showBackgroundLocationRationale()
            }
        }

        // Launcher for notification permission (Android 13+)
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Even if notification permission is denied, we can still proceed
            // with location tracking, just without showing notifications
            onPermissionsResult?.invoke(true)
        }
    }


    fun requestLocationPermissions(
        requireBackgroundLocation: Boolean = true,
        resultCallback: (Boolean) -> Unit
    ) {
        onPermissionsResult = resultCallback
        needsBackgroundLocationRequest = requireBackgroundLocation

        // Check if we already have the initial permissions
        if (hasInitialLocationPermissions()) {
            // If we need background location and don't have it, request it
            if (requireBackgroundLocation &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !hasBackgroundLocationPermission()) {
                requestBackgroundLocationPermission()
            }
            // If we need notification permission and don't have it, request it
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasNotificationPermission()) {
                requestNotificationPermission()
            }

            else {
                resultCallback(true)
            }
            return
        }

        // First time permission request or some initial permissions are missing
        showLocationPermissionRationale()
    }

    /**
     * Request initial location permissions (ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION)
     * plus other necessary permissions except background location
     */
    private fun requestInitialPermissions() {
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Add foreground service permission for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissionsToRequest.add(android.Manifest.permission.FOREGROUND_SERVICE)
        }

        // Add notification permission for Android 13+ (will request separately)

        // Request the permissions
        locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    /**
     * Request background location permission separately (required on Android 10+)
     */
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Show explanation dialog before requesting background location
            AlertDialog.Builder(activity)
                .setTitle("Background Location Access Needed")
                .setMessage("This app needs to access your location in the background to track your location even when the app is closed or in the background.")
                .setPositiveButton("OK") { _, _ ->
                    backgroundLocationPermissionLauncher.launch(
                        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
                .setNegativeButton("Cancel") { _, _ ->
                    onPermissionsResult?.invoke(false)
                }
                .create()
                .show()
        }
    }

    /**
     * Request notification permission (required on Android 13+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Show rationale dialog for location permissions
     */
    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(activity)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permissions to track your location. Without these permissions, the app cannot function properly.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestInitialPermissions()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onPermissionsResult?.invoke(false)
            }
            .create()
            .show()
    }

    /**
     * Show rationale dialog when background location is denied
     */
    private fun showBackgroundLocationRationale() {
        AlertDialog.Builder(activity)
            .setTitle("Background Location Needed")
            .setMessage("Without background location permission, this app can only track your location when it's open. Would you like to enable this permission in settings?")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                openAppSettings()
            }
            .setNegativeButton("No Thanks") { _, _ ->
                onPermissionsResult?.invoke(false)
            }
            .create()
            .show()
    }

    /**
     * Open the app settings page
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    /**
     * Check if we have the initial location permissions
     */
    private fun hasInitialLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if we have background location permission
     */
    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Before Android 10, background location is included with fine location
            hasInitialLocationPermissions()
        }
    }

    /**
     * Check if we have notification permission
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}