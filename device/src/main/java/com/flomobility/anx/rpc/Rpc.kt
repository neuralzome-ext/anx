package com.flomobility.anx.rpc

import com.google.protobuf.Message

abstract class Rpc<Req: Message, Rep: Message> {

    abstract val name: String

    abstract fun execute(req: Req): Rep

    abstract fun execute(req: ByteArray): Rep

}
