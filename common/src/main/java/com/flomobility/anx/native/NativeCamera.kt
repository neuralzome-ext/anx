package com.flomobility.anx.native

import com.google.protobuf.ByteString

object NativeCamera {

    init {
        System.loadLibrary("anx_camera")
    }

    external fun initCam(address: String): Long

    external fun startCam(camOptions: ByteArray)

    external fun getStreams(): ByteArray

    external fun stopCam()

    external fun destroyCam()



}
