# Location Tracking Service with Geofencing

## Overview
This project implements a **Location Tracking Service** that runs in the background and provides real-time location updates using the **Fused Location Provider**. The service is optimized for battery efficiency and is compliant with Android 12+ background execution policies. Additionally, it includes **Geofencing** to trigger events when a user enters or exits a predefined location.

## Features
- Uses **Fused Location Provider** for high-accuracy location tracking.
- Runs in the background using a **Foreground Service**.
- Provides real-time updates via **Flow** and **LiveData**.
- Optimized for battery efficiency using **WorkManager constraints**.
- Compliant with **Android 12+ background location restrictions**.
- Implements **Geofencing** to detect entry/exit events.

## Project Structure
```
locationTracking (Package Name)
│── MainActivity.kt (Handles UI and starts/stops tracking)
│── LocationTrackingService.kt (Foreground service for location updates)
│── PermissionsHandler.kt (Manages permission requests)
│── LocationWorkManager.kt (Optimizes location updates using WorkManager)
│── LocationManager.kt (Manages FusedLocationProviderClient)
│── GeofenceBroadcastReceiver.kt (Receives geofence transition events)
```

## Setup Instructions


```

### 1. Add Required Permissions
Ensure the following permissions are added in **AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

For Android 13+:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 2. Enable the Foreground Service
Declare the **Foreground Service** in **AndroidManifest.xml**:
```xml
<service
    android:name=".service.LocationTrackingService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

### 3. Implement Fused Location Provider
- Use `FusedLocationProviderClient` to fetch location updates.
- Start tracking in a foreground service.

### 4. Implement Geofencing
- Define geofence regions.
- Register geofences using `GeofencingClient`.
- Handle events in `GeofenceBroadcastReceiver.kt`.

## Usage

### Start Location Tracking
Click the **Start Tracking** button in `MainActivity.kt`, or call:
```kotlin
startService(Intent(this, LocationTrackingService::class.java))
```

### Stop Location Tracking
Click the **Stop Tracking** button in `MainActivity.kt`, or call:
```kotlin
stopService(Intent(this, LocationTrackingService::class.java))
```

### Debugging Geofencing
Use ADB to trigger a geofence event manually:
```sh
adb shell am broadcast -a android.location.GEOFENCE_TRANSITION --es transition "ENTER"
```

## Troubleshooting
### Geofencing Not Working?
- Ensure location permissions are granted.
- Check if **Google Play services** are enabled.
- Verify that background location access is allowed.
- Check logs for errors related to `GeofencingClient`.


