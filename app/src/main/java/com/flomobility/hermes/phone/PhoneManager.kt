package com.flomobility.hermes.phone

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.flomobility.hermes.other.getRootOutput
import com.flomobility.hermes.other.runAsRoot
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton


@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class PhoneManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Inject
    lateinit var device: Device

    private var sLastCpuCoreCount = -1

    private val telephony by lazy {
        context.getSystemService(
            TelephonyManager::class.java
        ) as TelephonyManager
    }

    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    fun getIdentity(): String {
        val telemamanger =
            context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE) as TelephonyManager
        val getSimNumber = telemamanger.line1Number

        Timber.d("FLOID $getSimNumber")
        return getSimNumber
//        return "${telephony.getImei(0)}:${telephony.getImei(1)}"
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

    fun getCpu(): Double {
        val currentFreq = arrayListOf<Double>()
        for (i in 0 until calcCpuCoreCount()) {
            currentFreq.add(takeCurrentCpuFreq(i))
        }
        return currentFreq.average()
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
            -1.0
        }
    }

    private fun readIntegerFile(filePath: String): Double {
        return try {
            val reader = BufferedReader(
                InputStreamReader(FileInputStream(filePath)), 1000
            )
            val line = reader.readLine()
            reader.close()
            line.toDouble() / 1000
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            -1.00
        }
    }

    private fun takeCurrentCpuFreq(coreIndex: Int): Double {
        val curFreq =
            readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
        val minFreq =
            readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_min_freq")
        val maxFreq =
            readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_max_freq")
        val currentFreq = ((curFreq - minFreq) * 100 / (maxFreq - minFreq))
        Timber.d(
            "FLOCPU $coreIndex $currentFreq ${readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_min_freq")} ${
                readIntegerFile(
                    "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq"
                )
            } ${readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_max_freq")}"
        )
//        return readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
        return curFreq
    }

    private fun calcCpuCoreCount(): Int {
        if (sLastCpuCoreCount >= 1) {
            // キャッシュさせる
            return sLastCpuCoreCount
        }
        sLastCpuCoreCount = try {
            // Get directory containing CPU info
            val dir = File("/sys/devices/system/cpu/")
            // Filter to only list the devices we care about
            val files: Array<File>? =
                dir.listFiles { pathname -> //Check if filename is "cpu", followed by a single digit number
                    Pattern.matches("cpu[0-9]", pathname.name)
                }

            // Return the number of cores (virtual CPU devices)
            files!!.size
        } catch (e: java.lang.Exception) {
            Runtime.getRuntime().availableProcessors()
        }
        Timber.d("FLOCPU $sLastCpuCoreCount")
        return sLastCpuCoreCount
    }

    fun invokeSignal(signal: Int) {
        Thread({
            when (signal) {
                SIG_SHUTDOWN -> {
                    Timber.i("[OS] --  Shutting Down...")
                    if (device.isRooted)
                        runAsRoot("reboot -p")
                    else
                        Timber.e("Can't shutdown as system isn't rooted")
                }
            }
        }, "execute-signal-$signal-thread").start()
    }

    companion object Signals {
        const val SIG_SHUTDOWN = 0
    }

}