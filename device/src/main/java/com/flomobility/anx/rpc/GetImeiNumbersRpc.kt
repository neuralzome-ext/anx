package com.flomobility.anx.rpc

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.phone.PhoneManager
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetImeiNumbersRpc @Inject constructor(
    private val phoneManager: PhoneManager
) : Rpc<Common.Empty, Device.GetImeiNumbersResponse>() {

    override val name: String
        get() = "GetImeiNumbers"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun execute(req: Common.Empty): Device.GetImeiNumbersResponse {
        return phoneManager.getImei()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun execute(req: ByteArray): Device.GetImeiNumbersResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
