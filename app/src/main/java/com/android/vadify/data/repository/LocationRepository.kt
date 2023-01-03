package com.android.vadify.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.android.vadify.data.extension.checkLocationPermission
import com.android.vadify.data.extension.getLocationManager
import com.android.vadify.data.extension.isGPSEnabled
import com.android.vadify.data.extension.isNetworkEnabled
import javax.inject.Inject

@SuppressLint("MissingPermission")
class LocationRepository @Inject constructor(
    private val context: Context
) {


    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 0f // 10 meters
        private const val MIN_TIME_BW_UPDATES = 0L //1000 * 60 * 1 // 1 minute
    }

    fun getUserCurrentLocation(response: ((Location?) -> Unit)? = null) {
        val locationManager = context.getLocationManager()
        when {
            context.isNetworkEnabled() -> {
                val location =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (location != null) {
                    response?.invoke(location)
                } else locationManager.updateLocation(LocationManager.NETWORK_PROVIDER) {
                    response?.invoke(it)
                }
            }
            context.isGPSEnabled() -> {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    response?.invoke(location)
                } else locationManager.updateLocation(LocationManager.GPS_PROVIDER) {
                    response?.invoke(it)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun LocationManager.updateLocation(
        locationType: String,
        onSuccess: (Location?) -> Unit
    ) {
        if (context.checkLocationPermission()) {
            this.requestLocationUpdates(
                locationType,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        removeUpdates(this)
                        onSuccess.invoke(location)
                    }

                    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                }
            )
        }
    }
}

