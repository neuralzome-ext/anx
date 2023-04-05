package com.flomobility.anx.native

object NativeTfLiteRunnerServer {

    init {
        System.loadLibrary("anx_tflite_runner")
    }

    const val DELEGATE_CPU = 0
    const val DELEGATE_GPU = 1
    const val DELEGATE_DSP = 2

    external fun init(address: String, delegate: Int)

    external fun start()

    external fun initAll(address: String)

    external fun startAll()
}
