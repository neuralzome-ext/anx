package com.flomobility.anx.rpc

import com.flomobility.anx.BuildConfig
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAnxVersionRpc @Inject constructor() :
    Rpc<Common.Empty, Device.VersionResponse>() {

    private fun getAnxVersion(): String {
        return BuildConfig.APP_VERSION_NAME
    }

    override val name: String
        get() = "GetAnxVersion"

    override fun execute(req: Common.Empty): Device.VersionResponse {
        val versionResponse = Device.VersionResponse.newBuilder()
        versionResponse.version = getAnxVersion()
        return versionResponse.build()
    }

    override fun execute(req: ByteArray): Device.VersionResponse {
        return execute(Common.Empty.parseFrom(req))
    }

}
