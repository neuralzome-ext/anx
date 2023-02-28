package com.flomobility.anx.phone

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.flomobility.anx.proto.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.M)
@Singleton
class PhoneManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "PhoneManager"
    }

    private val imeiNumber = Device.GetImeiNumbersResponse.newBuilder()
    private val imeis = ArrayList<String>()

    private val telephony by lazy {
        context.getSystemService(
            TelephonyManager::class.java
        ) as TelephonyManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getImei(): Device.GetImeiNumbersResponse {
        imeis.clear()
        imeis.add(telephony.getImei(0))
        imeis.add(telephony.getImei(1))
        imeiNumber.apply {
            clearImeis()
            addAllImeis(imeis)

        }
        Timber.tag(TAG).d("Imei list $imeis")
        return imeiNumber.build()
    }
}
