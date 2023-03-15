package com.flomobility.anx.native.zmq

import com.flomobility.anx.native.NativeZmq

class Publisher {

    private var publisherPtr: Long = 0L

    fun init(address: String) {
        publisherPtr = NativeZmq.createPublisherInstance(address)
    }

    fun publish(bytes: ByteArray) {
        NativeZmq.sendData(publisherPtr, bytes)
    }

    fun close() {
        NativeZmq.closePublisher(publisherPtr)
    }

}
