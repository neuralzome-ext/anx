//
// Created by Clay-Flo on 24/03/23.
//

#include <jni.h>
#include <sstream>
#include <memory>
#include "sensor/imu.h"

#define TAG "SensorListener"

std::unique_ptr<Imu> device_imu_;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flomobility_anx_native_NativeSensors_initImu(
        JNIEnv *env,
        jobject thiz,
        jint fps,
        jstring address) {
    const char *cstr = env->GetStringUTFChars(address, NULL);
    std::string _address(cstr);
    env->ReleaseStringUTFChars(address, cstr);

    device_imu_ = std::make_unique<Imu>(/*fps*/fps, _address);
    return (jlong) device_imu_.get();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeSensors_startImu(
        JNIEnv *env,
        jobject thiz) {
    device_imu_->start();

}
extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeSensors_stopImu(
        JNIEnv *env,
        jobject thiz) {
    device_imu_->stop();
}
