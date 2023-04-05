package com.flomobility.anx.utils

import android.content.Context
import java.io.File

object AddressUtils {

    fun getNamedPipeAddress(context: Context, name: String): String {
        val ipcDir = "${context.filesDir}/ipc"
        if(!dirExists(ipcDir)) createDir(ipcDir)
        return "ipc://$ipcDir/$name"
    }

    fun getRootNamedPipe(context: Context, name: String): String {
        val ipcDir = "${context.filesDir}/ipc"
        if(!dirExists(ipcDir)) createDir(ipcDir)
        return "ipc://$ipcDir"
    }

    private fun dirExists(dir: String): Boolean {
        val file = File(dir)
        return file.isDirectory
    }

    private fun createDir(dir: String) {
        val file = File(dir)
        file.mkdirs()
    }
}
