package com.flomobility.anx.rpc

import android.os.Build
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun execute(req: Common.Empty): Assets.AssetState {
        return assetManager.getAssetState()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun execute(req: ByteArray): Assets.AssetState {
        return execute(Common.Empty.parseFrom(req))
    }
}
