package com.flomobility.anx.hermes.phone

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.*
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.flomobility.anx.hermes.api.model.PhoneStates
import com.flomobility.anx.hermes.common.Result
import com.flomobility.anx.hermes.other.getDeviceID
import com.flomobility.anx.hermes.other.isHeadLessBuildType
import com.flomobility.anx.hermes.other.runAsRoot
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.*
import java.lang.Process
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton


@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class PhoneManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences
) {

    @Inject
    lateinit var device: Device

    private var sLastCpuCoreCount = -1
    private var sLastThermalCount = -1

    private val telephony by lazy {
        context.getSystemService(
            TelephonyManager::class.java
        ) as TelephonyManager
    }

    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    @SuppressLint("MissingPermission")
    fun getIdentity(): String {
//        val telemamanger =
//            context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE) as TelephonyManager
//        val getSimNumber = telemamanger.line1Number
        var identity = sharedPreferences.getDeviceID()
        if (isHeadLessBuildType()) {
            identity = "${telephony.getImei(0)}:${telephony.getImei(1)}"
        }
        Timber.d("FLOID $identity")
        return identity!!
//        return "${telephony.getImei(0)}:${telephony.getImei(1)}"
    }

    fun getBatteryInfo(): PhoneStates.Battery {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
//        Timber.d("FLO Battery ${batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)} ${batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)} ${intent?.getIntExtra(
//            BatteryManager.ACTION_CHARGING, -10)}")
        val batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
//        return (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
//            || batteryStatus == BatteryManager.BATTERY_STATUS_FULL)
        return PhoneStates.Battery(
            (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
                || batteryStatus == BatteryManager.BATTERY_STATUS_FULL),
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000,
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            intent?.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE, -1
            ) ?: -1
        )
    }

    /**
     * Function to get the current runtime memory info.
     *
     * @param context for accessing getSystemService method.
     * @return percent of memory used by the system.
     */
    fun getMemoryInfo(): PhoneStates.Memory {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)//Code not being used
        return PhoneStates.Memory(
            total = memoryInfo.totalMem,
            used = memoryInfo.totalMem - memoryInfo.availMem
        )
    }

    fun getStorage(): PhoneStates.Memory {
        val stat = StatFs(Environment.getDataDirectory().path)
        return PhoneStates.Memory(
            used = (stat.totalBytes - stat.availableBytes),
            total = stat.totalBytes
        )
    }

    @Throws(IOException::class)
    fun getCPUInfo(): String {
        val br = BufferedReader(FileReader("/proc/cpuinfo"))
        var output = ""
        var line = br.readLine()
        while (line != null) {
            val data = line.split(":").toTypedArray()
            if (data.size > 1) {
                val key = data[0].trim { it <= ' ' }.replace(" ", "_")
                if (key == "Hardware") {
                    output = data[1].trim { it <= ' ' }
                    break
                }
            }
            line = br.readLine()
        }
        br.close()
        return output
    }

    /**
     * Function to get the battery temperature of device using Runtime command (require rooted device).
     *
     * @return battery temperature of type float.
     */
    fun getThermals(): ArrayList<PhoneStates.Thermal> {
        val thermals = arrayListOf<PhoneStates.Thermal>()
        for (i in 0 until calcThermalCount()) {
            val thermalProcess: Process
            val typeProcess: Process
            try {
                thermalProcess =
                    Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone$i/temp")
                typeProcess =
                    Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone$i/type")
                thermalProcess.waitFor()
                typeProcess.waitFor()
                val thermalReader = BufferedReader(InputStreamReader(thermalProcess.inputStream))
                val typeReader = BufferedReader(InputStreamReader(typeProcess.inputStream))
                val thermal = thermalReader.readLine()
                val type = typeReader.readLine()
                if (type != null) {
                    thermals.add(PhoneStates.Thermal(type, thermal.toDouble() / 1000))
                } else {
                    continue
                }
            } catch (e: Exception) {
                Timber.e(e.localizedMessage)
//            Timber.e(e.localizedMessage)
                continue
            }
        }
        return thermals
    }

    fun getSystemUptime(): Double {
        return (SystemClock.uptimeMillis() / 1000).toDouble()
    }

    fun getCurrentCpu(): ArrayList<PhoneStates.Cpu.CpuFreq> {
        val currentFreq = arrayListOf<PhoneStates.Cpu.CpuFreq>()
        for (i in 0 until calcCpuCoreCount()) {
            currentFreq.add(getCpuFreq(i))
        }
        return currentFreq
    }

    fun getGpuUsage(): PhoneStates.Memory {
        return PhoneStates.Memory(-1, -1)
/*        try {
            if (!device.isRooted) return -1.0

            val currentFreq: Double
            val curFreq = getRootOutput("cat /sys/class/kgsl/kgsl-3d0/clock_mhz")
            val minFreq = getRootOutput("cat /sys/class/kgsl/kgsl-3d0/min_clock_mhz")
            val maxFreq = getRootOutput("cat /sys/class/kgsl/kgsl-3d0/max_clock_mhz")
            currentFreq =
                ((curFreq.toDouble() - minFreq.toDouble()) * 100 / (maxFreq.toDouble() - minFreq.toDouble()))
//            Timber.d("GPUUSAGE Current Frequency of Cores is $currentFreq")
            return currentFreq
        } catch (ex: Exception) {
//            Timber.e("GPUUSAGE Core is Idle")
            ex.printStackTrace()
            return -1.0
        }*/
    }

    private fun readIntegerFile(filePath: String): Int {
        try {
            BufferedReader(
                InputStreamReader(FileInputStream(filePath)), 1000
            ).use { reader ->
                val line = reader.readLine()
                return Integer.parseInt(line)
            }
        } catch (e: Exception) {
            // 冬眠してるコアのデータは取れないのでログを出力しない
            //Timber.e(e);
            return 0
        }
    }

    private fun getCpuFreq(coreIndex: Int): PhoneStates.Cpu.CpuFreq {
        val curFreq =
            readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq").toDouble()
        val minFreq =
            readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_min_freq").toDouble()
        val maxFreq =
            readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq").toDouble()
        return PhoneStates.Cpu.CpuFreq(curFreq, maxFreq, minFreq)
//        val currentFreq = ((curFreq - minFreq).toDouble() / (maxFreq - minFreq).toDouble())
//        Timber.d("CPU $coreIndex -- $curFreq, $minFreq, $maxFreq")
    }

    private fun calcThermalCount(): Int {
        if (sLastThermalCount >= 1) {
            // キャッシュさせる
            return sLastThermalCount
        }
        sLastThermalCount = try {
            // Get directory containing CPU info
            val dir = File("/sys/class/thermal/")
            // Filter to only list the devices we care about
            val files: Array<File>? =
                dir.listFiles { pathname -> //Check if filename is "thermal", followed by a number
                    Pattern.matches("thermal_zone[0-9]+$", pathname.name)
                }
            // Return the number of thermals
            files!!.size
        } catch (e: java.lang.Exception) {
            Runtime.getRuntime().availableProcessors()
        }
        return sLastThermalCount
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

    /**/
    fun invokeSignal(signal: Int): Result {
        /*Thread({
            when (signal) {
                SIG_SHUTDOWN -> {
                    Timber.i("[OS] --  Shutting Down...")
                    if (device.isRooted)
                        runAsRoot("reboot -p")
                    else
                        Timber.e("Can't shutdown as system isn't rooted")
                }
                else -> {
                    Timber.e("Unknown signal")
                }
            }
        }, "execute-signal-$signal-thread").start()*/
        return when (signal) {
            SIG_SHUTDOWN -> {
                if (device.isRooted) {
                    Timber.i("[OS] --  Shutting Down...")
                    runAsRoot("reboot -p")
                    Result(success = true)
                } else Result(
                    success = false,
                    message = "Can't shutdown. Device needs to be rooted."
                )
            }
            else -> {
                Timber.e("Unknown signal")
                Result(success = false, message = "Unknown signal...")
            }
        }
    }

    companion object Signals {
        const val SIG_SHUTDOWN = 0
    }

}
