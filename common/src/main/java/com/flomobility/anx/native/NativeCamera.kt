package com.flomobility.anx.native

object NativeCamera {

    init {
        System.loadLibrary("anx_camera")
    }

    external fun initCam(address: String): Long

    external fun startCam()

    external fun stopCam()

    external fun destroyCam()



}
