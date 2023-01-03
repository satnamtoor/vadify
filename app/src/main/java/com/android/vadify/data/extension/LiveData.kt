package com.android.vadify.data.extension

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData

fun <TSOURCE, TOUT> mediatorLiveData(
    source: LiveData<TSOURCE>,
    initial: TOUT? = null,
    onChanged: MediatorLiveData<TOUT>.(TSOURCE?) -> Unit
): MediatorLiveData<TOUT> {
    val liveData = MediatorLiveData<TOUT>()
    initial?.let { liveData.postValue(it) }
    liveData.addSource(source) { onChanged(liveData, it) }
    return liveData
}

fun <T> liveData(value: T?): LiveData<T> {
    return mutableLiveData(value)
}


inline fun <TSOURCE, TOUT> mediatorLiveDataUpdate(
    source: LiveData<TSOURCE>,
    crossinline onChanged: (TSOURCE) -> TOUT
): MediatorLiveData<TOUT> {
    val liveData = MediatorLiveData<TOUT>()
    liveData.addSource(source) {
        liveData.postValue(onChanged(it))
    }
    return liveData
}


fun Context.getLocationManager() = (getSystemService(Context.LOCATION_SERVICE) as LocationManager)

fun Context.checkLocationPermission(): Boolean =
    this.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && this.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.isGPSEnabled() =
    (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(
        LocationManager.GPS_PROVIDER
    )

fun Context.isNetworkEnabled() =
    (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
    )
