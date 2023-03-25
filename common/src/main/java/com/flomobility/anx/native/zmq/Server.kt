package com.flomobility.anx.native.zmq

import com.flomobility.anx.native.Message
import com.flomobility.anx.native.NativeZmq
import com.flomobility.anx.native.RpcPayload

class Server {

    private var serverPtr: Long = 0L

    fun init(address: String) {
        serverPtr = NativeZmq.createServerInstance(address)
    }

    fun listen(): Message {
        return NativeZmq.listenServerRequests(serverPtr)
    }

    fun send(byteArray: ByteArray) {
        NativeZmq.sendServerResponse(serverPtr, byteArray)
    }

    fun close(): Boolean {
        return NativeZmq.closeServer(serverPtr)
    }

}
