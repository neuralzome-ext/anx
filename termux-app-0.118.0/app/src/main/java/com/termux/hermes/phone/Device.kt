package com.termux.hermes.phone

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Device @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var isRooted: Boolean = false
        private set

    fun checkIsRooted() {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("su")
            isRooted = true
        } catch (e: Exception) {
            isRooted = false
        } finally {
            if (process != null) {
                try {
                    process.destroy()
                } catch (e: Exception) {
                }
            }
        }
    }
}
