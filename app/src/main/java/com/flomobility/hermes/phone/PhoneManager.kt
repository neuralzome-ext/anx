package com.flomobility.hermes.phone

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class PhoneManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val telephony by lazy {
        context.getSystemService(
            TelephonyManager::class.java
        ) as TelephonyManager
    }

    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    fun getIdentity(): String {
        return "${telephony.getImei(0)}:${telephony.getImei(1)}"
    }

    fun getChargingStatus(): Boolean {
        val batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
            || batteryStatus == BatteryManager.BATTERY_STATUS_FULL)
    }

}