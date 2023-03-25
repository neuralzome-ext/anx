package com.flomobility.anx.rpc

import com.flomobility.anx.assets.AssetManager
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartDeviceImuRpc @Inject constructor(
    private val assetManager: AssetManager
) : Rpc<Assets.StartDeviceImu, Common.StdResponse>() {

    override val name: String
        get() = "StartDeviceImu"

    override fun execute(req: Assets.StartDeviceImu): Common.StdResponse {
        return assetManager.startDeviceImu(req)
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Assets.StartDeviceImu.parseFrom(req))
    }
}
