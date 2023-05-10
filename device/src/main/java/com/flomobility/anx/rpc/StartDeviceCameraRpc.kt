package com.flomobility.anx.rpc

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.assets.AssetManager
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.N)
class StartDeviceCameraRpc @Inject constructor(
    private val assetManager: AssetManager
) : Rpc<Assets.StartDeviceCamera, Common.StdResponse>() {

    override val name: String
        get() = "StartDeviceCamera"

    override fun execute(req: Assets.StartDeviceCamera): Common.StdResponse {
       return assetManager.startDeviceCamera(req)
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Assets.StartDeviceCamera.parseFrom(req))
    }
}
