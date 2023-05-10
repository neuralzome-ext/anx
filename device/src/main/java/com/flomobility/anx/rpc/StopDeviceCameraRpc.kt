package com.flomobility.anx.rpc

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.assets.AssetManager
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.N)
class StopDeviceCameraRpc @Inject constructor(
    private val assetManager: AssetManager
) : Rpc<Common.Empty, Common.StdResponse>() {

    override val name: String
        get() = "StopDeviceCamera"

    override fun execute(req: Common.Empty): Common.StdResponse {
        return assetManager.stopDeviceCamera()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
