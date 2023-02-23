package com.flomobility.anx.assets.imu

import com.flomobility.anx.assets.Asset
import com.flomobility.anx.proto.Assets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceImu @Inject constructor(): Asset() {

    fun getDeviceImuSelect(): Assets.DeviceImuSelect {
        return Assets.DeviceImuSelect.newBuilder().apply {
            this.addAllFps(getAvailableFps())
        }.build()
    }

    private fun getAvailableFps(): List<Int> {
       return listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)
    }

    override fun start(): Boolean {
        return true
    }

    override fun stop(): Boolean {
        return true
    }
}
