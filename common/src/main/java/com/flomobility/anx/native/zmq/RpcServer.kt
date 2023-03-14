package com.flomobility.anx.native.zmq

import com.flomobility.anx.native.NativeZmq
import com.flomobility.anx.native.RpcPayload

class RpcServer {

    private var serverPtr: Long = 0L

    fun init(address: String) {
        serverPtr = NativeZmq.createServerInstance(address)
    }

    fun listen(): RpcPayload {
        return NativeZmq.listenForRpcs(serverPtr)
    }

    fun send(byteArray: ByteArray) {
        NativeZmq.sendServerResponse(serverPtr, byteArray)
    }

    fun close(): Boolean {
        return NativeZmq.closeServer(serverPtr)
    }

}
