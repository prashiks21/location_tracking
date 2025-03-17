package com.example.locationtracking.workManager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton manager that tracks the current geofence status.
 * Similar to LocationTrackingServiceManager, it provides observable status
 * for UI components to update based on geofence transitions.
 */
object GeofenceStatusManager {
    // Data model for geofence status
    data class GeofenceStatus(
        val state: String,
        val geofenceIds: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Initial status
    private val initialStatus = GeofenceStatus("INACTIVE", "")

    // LiveData for UI updates
    private val _statusLiveData = MutableLiveData<GeofenceStatus>(initialStatus)
    val statusLiveData: LiveData<GeofenceStatus> = _statusLiveData

    // Flow for UI updates
    private val _statusFlow = MutableStateFlow<GeofenceStatus>(initialStatus)
    val statusFlow: StateFlow<GeofenceStatus> = _statusFlow

    /**
     * Updates the geofence status when transitions occur
     * @param state The new state (ENTERED, EXITED, etc.)
     * @param geofenceIds The IDs of the geofences involved
     */
    fun updateStatus(state: String, geofenceIds: String) {
        val newStatus = GeofenceStatus(state, geofenceIds)

        // Update both LiveData and Flow
        _statusLiveData.postValue(newStatus)
        _statusFlow.value = newStatus
    }

    /**
     * Reset status to inactive (e.g., when stopping geofence monitoring)
     */
    fun resetStatus() {
        _statusLiveData.postValue(initialStatus)
        _statusFlow.value = initialStatus
    }
}