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
        JNIEnv *env, jobject thiz) {
    cam->start();
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
