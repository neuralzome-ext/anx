package com.flomobility.anx.native

object NativeSensors {

    init {
        System.loadLibrary("anx")
    }

    external fun initImu(fps: Int, address: String): Long

    external fun startImu()

    external fun stopImu()

}
