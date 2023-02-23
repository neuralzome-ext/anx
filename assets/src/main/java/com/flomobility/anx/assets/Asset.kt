package com.flomobility.anx.assets

import com.flomobility.anx.common.Result
import com.google.protobuf.Message

abstract class Asset<Options: Message> {

    abstract fun start(options: Options?): Result

    abstract fun stop(): Result

}
