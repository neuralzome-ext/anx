//
// Created by Clay-Flo on 06/05/23.
//


#include <jni.h>
#include "camera/camera.h"

std::unique_ptr<NdkCamera> cam;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flomobility_anx_native_NativeCamera_initCam(
        JNIEnv *env,
        jobject thiz, jstring addr) {
    const char *cstr = env->GetStringUTFChars(addr, NULL);
    std::string address(cstr);
    env->ReleaseStringUTFChars(addr, cstr);

    cam = std::make_unique<NdkCamera>(address);
    cam->init();
    return (jlong) cam.get();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeCamera_startCam(
        JNIEnv *env, jobject thiz, jbyteArray start_config) {
    jbyte* data = env->GetByteArrayElements(start_config, nullptr);
    jsize length = env->GetArrayLength(start_config);

    std::string serializedString(reinterpret_cast<char*>(data), length);

    anx::StartDeviceCamera start_camera_config = NdkCamera::Bytes2Stream(serializedString);
    cam->Start(start_camera_config);

    env->ReleaseByteArrayElements(start_config, data, JNI_ABORT);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeCamera_stopCam(JNIEnv *env, jobject thiz) {
    cam->stop();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeCamera_destroyCam(JNIEnv *env, jobject thiz) {
    cam = nullptr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_flomobility_anx_native_NativeCamera_getStreams(
        JNIEnv *env, jobject thiz) {
    std::string serializedData = cam->streams.SerializeAsString();
// Convert the serialized string to a byte array
    jbyteArray byteArray = env->NewByteArray(serializedData.length());
    env->SetByteArrayRegion(byteArray, 0, serializedData.length(), reinterpret_cast<const jbyte*>(serializedData.c_str()));

    return byteArray;
}
