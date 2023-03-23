package com.flomobility.anx.logs

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import com.flomobility.anx.common.Result
import com.flomobility.anx.other.Constants
import com.flomobility.anx.other.runAsRoot
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLogsUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * [fileName] - name of log file, would be visible in the /logs dir
     * @return standard [Result] response
     * */
    fun startLogging(fileName: String): Result {
        return try {
            val target = "/logs/$fileName"
            val cmd = "su -c mount -o rw,remount /;su -c logcat >> $target &"
            val logsProcess = Runtime.getRuntime().exec(cmd)
            val code = logsProcess.waitFor()
//            readRootOutput(p)
            Timber.i("Logging to $target started. Exited: $code, stdout : ${logsProcess.inputStream.bufferedReader().readLines()}, stderr : ${logsProcess.errorStream.bufferedReader().readLines()}")
            Result(success = true)
        } catch (e: Exception) {
            Timber.e(e)
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }
    }

    @SuppressLint("NewApi")
    fun startLogging(): Result {
        try {
            val c = Calendar.getInstance().time
            val df = SimpleDateFormat("dd-MMM-yyyy-HH:mm", Locale.getDefault())
            val formattedDate: String = df.format(c)

            val systemLogPath = "system"
            val systemLogsDir = File("/logs" + File.separator + systemLogPath)

            if(!systemLogsDir.exists()) {
                runAsRoot("mount -o rw,remount /")
                runAsRoot("mkdir -p /logs/system")
            }
            val fileName = systemLogPath + File.separator + "$formattedDate.log"
            return startLogging(fileName)
        } catch (e: Exception) {
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }
    }

    fun stopLogging(): Result {
        return try {
            val cmd = "killall logcat"
            val logsProcess = Runtime.getRuntime().exec(cmd)
            val code = logsProcess.waitFor()
            Result(success = true)
        } catch (e: Exception) {
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }
    }


}
