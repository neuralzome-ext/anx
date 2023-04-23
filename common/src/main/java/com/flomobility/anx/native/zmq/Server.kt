package com.flomobility.anx.native.zmq

import com.flomobility.anx.native.Message
import com.flomobility.anx.native.NativeZmq
import com.flomobility.anx.native.RpcPayload

class Server {

    private var serverPtr: Long = 0L

    fun init(address: String) {
        serverPtr = NativeZmq.createServerInstance(address)
    }

    fun listen(): Boolean {
        return NativeZmq.listenServerRequests(serverPtr)
    }

    fun newMessage(): Message {
        return NativeZmq.getNewMessage(serverPtr)
    }

    fun send(byteArray: ByteArray) {
        NativeZmq.sendServerResponse(serverPtr, byteArray)
    }

    fun close(): Boolean {
        return NativeZmq.closeServer(serverPtr)
    }

}
