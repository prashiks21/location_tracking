package com.example.locationtracking.workManager

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocationTrackingServiceManager {
    // LiveData for UI updates
    private val _locationLiveData = MutableLiveData<Location?>()
    val locationLiveData: LiveData<Location?> = _locationLiveData

    // Flow for UI updates
    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow

    // Method to update location (called from LocationTrackingService)
    fun updateLocation(location: Location) {
        _locationLiveData.postValue(location)
        _locationFlow.value = location
    }

    fun clearLocation() {
        _locationLiveData.postValue(null)
        _locationFlow.value = null
    }
}