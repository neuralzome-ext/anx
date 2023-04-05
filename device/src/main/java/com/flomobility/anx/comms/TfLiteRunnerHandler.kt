package com.flomobility.anx.comms

import android.content.Context
import com.flomobility.anx.native.NativeTfLiteRunnerServer
import com.flomobility.anx.utils.AddressUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TfLiteRunnerHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun init() {
        /*NativeTfLiteRunnerServer.init(
            AddressUtils.getNamedPipeAddress(context, "cpu"),
            NativeTfLiteRunnerServer.DELEGATE_CPU
        )
        NativeTfLiteRunnerServer.start()*/

        NativeTfLiteRunnerServer.init(
            AddressUtils.getNamedPipeAddress(context, "gpu"),
            NativeTfLiteRunnerServer.DELEGATE_GPU
        )
        NativeTfLiteRunnerServer.start()

        /*NativeTfLiteRunnerServer.init(
            AddressUtils.getNamedPipeAddress(context, "dsp"),
            NativeTfLiteRunnerServer.DELEGATE_DSP
        )
        NativeTfLiteRunnerServer.start()*/
    }

}
