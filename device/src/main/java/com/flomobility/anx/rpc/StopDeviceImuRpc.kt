package com.flomobility.anx.rpc

import com.flomobility.anx.assets.AssetManager
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopDeviceImuRpc @Inject constructor(
    private val assetManager: AssetManager
) : Rpc<Common.Empty, Common.StdResponse>() {

    override val name: String
        get() = "StopDeviceImu"

    override fun execute(req: Common.Empty): Common.StdResponse {
        return assetManager.stopDeviceImu()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
