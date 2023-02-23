package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFloOsVersionRpc @Inject constructor() :
    Rpc<Common.Empty, Device.VersionResponse>() {

    private fun getFloOsVersion(): String {
        return "0.9.0" // TODO : change this
    }

    override val name: String
        get() = "GetFloOsVersion"

    override fun execute(req: Common.Empty): Device.VersionResponse {
        val versionResponse = Device.VersionResponse.newBuilder()
        versionResponse.version = getFloOsVersion()
        return versionResponse.build()
    }

    override fun execute(req: ByteArray): Device.VersionResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
