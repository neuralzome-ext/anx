package com.flomobility.anx.assets.gnss

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.N)
@Singleton
class DeviceGnssManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TAG = "DeviceGNSSManager"
    }

    private val locationManager by lazy {
        context.getSystemService(
            LocationManager::class.java
        ) as LocationManager
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}

        override fun onLocationChanged(location: Location) {}

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }

    fun init(nmeaListener: OnNmeaMessageListener): Boolean {
        val isGpsProviderEnabled: Boolean =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsProviderEnabled) {
            try {
                registerNMEAListener(nmeaListener)
                if (!locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                    return false
                }
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    TimeUnit.SECONDS.toMillis(1L),
                    0.0f /* minDistance */,
                    locationListener
                )

            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            //TODO handle GPS off case
            return false
        }
        return true // to be checked properly
    }

    @SuppressLint("MissingPermission")
    private fun registerNMEAListener(nmeaListener: OnNmeaMessageListener) {
        locationManager.addNmeaListener(nmeaListener, null)
    }

    private fun unRegisterNMEAListener(nmeaListener: OnNmeaMessageListener) {
        return locationManager.removeNmeaListener(nmeaListener);
    }

    fun stop(nmeaListener: OnNmeaMessageListener) {
        unRegisterNMEAListener(nmeaListener)
        locationManager.removeUpdates(locationListener)
    }
}
