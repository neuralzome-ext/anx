package com.flomobility.anx.native

object NativeZmq {

    init {
        System.loadLibrary("anx")
    }

    external fun createPublisherInstance(address: String): Long

    external fun sendData(publisherPtr: Long, data: ByteArray)

    external fun closePublisher(publisherPtr: Long): Boolean


    // Subscriber related
    external fun createSubscriberInstance(address: String, topic: String): Long

    external fun listen(subscriberPtr: Long): ByteArray

    external fun closeSubscriber(subscriberPtr: Long): Boolean

    // Server related
    external fun createServerInstance(address: String): Long

    external fun listenServerRequests(serverPtr: Long): ByteArray

    external fun listenForRpcs(serverPtr: Long): RpcPayload

    external fun sendServerResponse(serverPtr: Long, data: ByteArray): Boolean

    external fun closeServer(serverPtr: Long): Boolean

}
