package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFloOsVersionRpc @Inject constructor() :
    Rpc<Common.Empty, Device.VersionResponse>() {

    private val floOsVersionResponse = Device.VersionResponse.newBuilder()

    private fun getFloOsVersion(): Device.VersionResponse {
        try {
            val versionProcess = Runtime.getRuntime().exec("getprop ro.lineage.version")
            val floOsVersion = BufferedReader(InputStreamReader(versionProcess.inputStream)).readLine()
            Timber.d(floOsVersion)
            floOsVersionResponse.apply {
                version = floOsVersion
            }
        } catch (e: IOException) {
            Timber.d(e)
        }
        return floOsVersionResponse.build()
    }

    override val name: String
        get() = "GetFloOsVersion"

    override fun execute(req: Common.Empty): Device.VersionResponse {
        return getFloOsVersion()
    }

    override fun execute(req: ByteArray): Device.VersionResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
