//
// Created by Clay-Flo on 01/03/23.
//

#include "ipc_transport.h"
#include <jni.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flomobility_anx_native_NativeZmq_createPublisherInstance(
        JNIEnv *env, jobject thiz, jstring address) {
    const char *cstr = env->GetStringUTFChars(address, NULL);
    std::string _address(cstr);
    env->ReleaseStringUTFChars(address, cstr);
    auto *pub = new Publisher(_address);
    return (jlong) pub;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeZmq_sendData(
        JNIEnv *env, jobject thiz,
        jlong publisher_ptr,
        jbyteArray data) {
    auto *pub = (Publisher*)publisher_ptr;
    jsize length = env->GetArrayLength(data);
    jbyte* jbyteData = env->GetByteArrayElements(data, NULL);
    BYTE* byteData = new BYTE[length];
    memcpy(byteData, jbyteData, length);
    pub->SendData(byteData, length);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flomobility_anx_native_NativeZmq_closePublisher(
        JNIEnv *env, jobject thiz, jlong publisher_ptr) {
    auto *pub = (Publisher*)publisher_ptr;
    bool status = pub->close();
    free(pub);
    return status;
}
