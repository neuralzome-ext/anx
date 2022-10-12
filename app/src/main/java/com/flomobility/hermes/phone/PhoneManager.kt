package com.flomobility.hermes.phone

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.flomobility.hermes.other.getRootOutput
import com.flomobility.hermes.other.runAsRoot
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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

    /**
     * Function to get the current runtime memory info.
     *
     * @param context for accessing getSystemService method.
     * @return percent of memory used by the system.
     */
    fun getMemoryInfo(): Double {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)//Code not being used
        return 100.0 - (memoryInfo.availMem * 100.0) / memoryInfo.totalMem
    }

    /**
     * Function to get the battery temperature of device using Runtime command (require rooted device).
     *
     * @return battery temperature of type float.
     */
    fun getCPUTemperature(): Double {
        val process: Process
        return try {
            process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone" + 0 + "/temp")
            process.waitFor()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            if (line != null) {
                line.toDouble() / 1000
            } else {
                25.0
            }
        } catch (e: Exception) {
            Timber.e(e.localizedMessage)
//            Timber.e(e.localizedMessage)
            0.0
        }
    }

    fun getCpuUsage(): Double {
        return try {
            val currentFreq: Double
            val curFreq: String =
                getRootOutput("cat /sys/devices/system/cpu/cpufreq/policy4/cpuinfo_cur_freq")
            val minFreq: String =
                getRootOutput("cat /sys/devices/system/cpu/cpufreq/policy4/cpuinfo_min_freq")
            val maxFreq: String =
                getRootOutput("cat /sys/devices/system/cpu/cpufreq/policy4/cpuinfo_max_freq")
//            Timber.d("CPUUSAGE Current Frequency of Core is ${curFreq.toDouble()} ${minFreq.toDouble()} ${maxFreq.toDouble()}")
            currentFreq =
                (curFreq.toDouble() - minFreq.toDouble()) * 100 / (maxFreq.toDouble() - minFreq.toDouble())
            Timber.d("CPUUSAGE Current Frequency of Cores is $currentFreq")
            currentFreq
//            0.0
        } catch (ex: IOException) {
            Timber.e("CPUUSAGE Core is Idle")
            ex.printStackTrace()
            0.0
        }
    }

    fun getGpuUsage(): Double {
        return try {
            val currentFreq: Double
            val curFreq = getRootOutput("cat /sys/class/kgsl/kgsl-3d0/clock_mhz")
            val minFreq = getRootOutput("cat /sys/class/kgsl/kgsl-3d0/min_clock_mhz")
            val maxFreq = getRootOutput("cat /sys/class/kgsl/kgsl-3d0/max_clock_mhz")
            currentFreq =
                ((curFreq.toDouble() - minFreq.toDouble()) * 100 / (maxFreq.toDouble() - minFreq.toDouble()))
            Timber.d("GPUUSAGE Current Frequency of Cores is $currentFreq")
            currentFreq
        } catch (ex: IOException) {
            Timber.e("GPUUSAGE Core is Idle")
            ex.printStackTrace()
            0.0
        }
    }

    fun invokeSignal(signal: Int) {
        Thread({
            when (signal) {
                Signals.SIG_SHUTDOWN -> {
                    Timber.i("[OS] --  Shutting Down...")
                    runAsRoot("reboot -p")
                }
            }
        }, "execute-signal-$signal-thread").start()
    }

    companion object Signals {
        const val SIG_SHUTDOWN = 0
    }

}