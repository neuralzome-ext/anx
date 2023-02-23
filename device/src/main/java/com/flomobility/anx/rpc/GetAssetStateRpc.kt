package com.flomobility.anx.rpc

import com.flomobility.anx.assets.AssetManager
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAssetStateRpc @Inject constructor(
    private val assetManager: AssetManager
) : Rpc<Common.Empty, Assets.AssetState>() {

    override val name: String
        get() = "GetAssetState"

    override fun execute(req: Common.Empty): Assets.AssetState {
        return assetManager.getAssetState()
    }

    override fun execute(req: ByteArray): Assets.AssetState {
        return execute(Common.Empty.parseFrom(req))
    }
}
