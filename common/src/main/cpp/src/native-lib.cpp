//
// Created by Clay-Flo on 01/03/23.
//

#include "ipc/ipc_transport.h"
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
    auto *pub = (Publisher *) publisher_ptr;
    jsize length = env->GetArrayLength(data);
    jbyte *jbyteData = env->GetByteArrayElements(data, NULL);
    BYTE *byteData = new BYTE[length];
    memcpy(byteData, jbyteData, length);
    pub->SendData(byteData, length);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flomobility_anx_native_NativeZmq_closePublisher(
        JNIEnv *env, jobject thiz, jlong publisher_ptr) {
    auto *pub = (Publisher *) publisher_ptr;
    bool status = pub->close();
    free(pub);
    return status;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flomobility_anx_native_NativeZmq_createSubscriberInstance(
        JNIEnv *env,
        jobject thiz,
        jstring address, jstring topic) {
    const char *cstr = env->GetStringUTFChars(address, NULL);
    std::string _address(cstr);
    env->ReleaseStringUTFChars(address, cstr);

    const char *cstr_topic = env->GetStringUTFChars(topic, NULL);
    std::string _topic(cstr);
    env->ReleaseStringUTFChars(address, cstr_topic);

    auto *sub = new Subscriber(_address, _topic);
    return (jlong) sub;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_flomobility_anx_native_NativeZmq_listen(
        JNIEnv *env,
        jobject thiz,
        jlong subscriber_ptr) {
    auto *sub = (Subscriber *) subscriber_ptr;
    bytes_t bytes = sub->listen();
    jbyteArray result = env->NewByteArray(bytes.size);

    // Copy the byte array to the jbyteArray
    env->SetByteArrayRegion(result, 0, bytes.size, reinterpret_cast<jbyte *>(bytes.data));
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flomobility_anx_native_NativeZmq_closeSubscriber(
        JNIEnv *env, jobject thiz,
        jlong subscriber_ptr) {
    auto *sub = (Subscriber *) subscriber_ptr;
    bool status = sub->close();
    free(sub);
    return status;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flomobility_anx_native_NativeZmq_createServerInstance(
        JNIEnv *env,
        jobject thiz,
        jstring address) {
    const char *cstr = env->GetStringUTFChars(address, NULL);
    std::string _address(cstr);
    env->ReleaseStringUTFChars(address, cstr);

    auto *server = new Server(_address);
    return (jlong) server;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_flomobility_anx_native_NativeZmq_listenServerRequests(
        JNIEnv *env,
        jobject thiz,
        jlong server_ptr) {
    auto *server = (Server *) server_ptr;
    seq_message_t msg = server->listen();
    bytes_t bytes = msg.data;
    jbyteArray result = env->NewByteArray((int)bytes.size);

    // Copy the byte array to the jbyteArray
    env->SetByteArrayRegion(result, 0, (int)bytes.size, (jbyte *)(bytes.data));
    jclass clazz = env->FindClass("com/flomobility/anx/native/Message");
    jmethodID midConstructor = env->GetMethodID(
            clazz, "<init>",
            "(ZZ[B)V");
    jobject message = env->NewObject(clazz, midConstructor, msg.success, msg.more, result);
    free(bytes.data);
    return message;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flomobility_anx_native_NativeZmq_closeServer(
        JNIEnv *env,
        jobject thiz,
        jlong server_ptr) {
    auto *server = (Server *) server_ptr;
    bool status = server->close();
    free(server);
    return status;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_flomobility_anx_native_NativeZmq_listenForRpcs(
        JNIEnv *env,
        jobject thiz,
        jlong server_ptr) {
    auto *server = (Server *) server_ptr;
    rpc_payload_t payload = server->listenRpc();

    jstring rpc_name = env->NewStringUTF(payload.rpc_name.c_str());
    bytes_t bytes = payload.data;
    jbyteArray data = env->NewByteArray(bytes.size);

    // Copy the byte array to the jbyteArray
    env->SetByteArrayRegion(data, 0, bytes.size, reinterpret_cast<jbyte *>(bytes.data));

    jclass clazz = env->FindClass("com/flomobility/anx/native/RpcPayload");
    jmethodID midConstructor = env->GetMethodID(
            clazz, "<init>",
            "(Ljava/lang/String;[B)V");
    jobject rpcPayload = env->NewObject(clazz, midConstructor, rpc_name, data);
    return rpcPayload;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flomobility_anx_native_NativeZmq_sendServerResponse(
        JNIEnv *env,
        jobject thiz,
        jlong server_ptr, jbyteArray data) {
    auto server = (Server *) server_ptr;

    jsize length = env->GetArrayLength(data);
    jbyte *jbyteData = env->GetByteArrayElements(data, NULL);
    BYTE *byteData = new BYTE[length];
    memcpy(byteData, jbyteData, length);

    bytes_t payload{};
    payload.data = byteData;
    payload.size = length;

    return server->sendResponse(payload);
}
