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

    //

}
