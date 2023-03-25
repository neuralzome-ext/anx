package com.flomobility.anx.rpc

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.assets.AssetManager
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartDeviceGnssRpc @Inject constructor(
    private val assetManager: AssetManager
) : Rpc<Common.Empty, Common.StdResponse>() {

    override val name: String
        get() = "StartDeviceGnss"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun execute(req: Common.Empty): Common.StdResponse {
        return assetManager.startDeviceGnss(req)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
