package com.example.locationtracking.viewModel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    val _locationLiveData = MutableLiveData<Location?>()
    val locationLiveData: LiveData<Location?> = _locationLiveData

    fun updateLocation(location: Location) {
        _locationLiveData.postValue(location)
    }
}