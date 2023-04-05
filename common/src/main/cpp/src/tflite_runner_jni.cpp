//
// Created by Clay-Flo on 26/03/23.
//

#include <jni.h>
#include <memory>
#include "tflite/tflite_runner_rpc_server.h"

std::unique_ptr<TfLiteRunnerRpcServer> rpc_server;
std::unique_ptr<TfLiteRunnerRpcServer> tflite_cpu_rpc_server;
std::unique_ptr<TfLiteRunnerRpcServer> tflite_gpu_rpc_server;
std::unique_ptr<TfLiteRunnerRpcServer> tflite_dsp_rpc_server;


extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeTfLiteRunnerServer_init(
        JNIEnv *env,
        jobject thiz,
        jstring address, jint delegate) {
    const char *cstr = env->GetStringUTFChars(address, NULL);
    std::string _address(cstr);
    env->ReleaseStringUTFChars(address, cstr);

    TfliteRunner::DelegateType delegateType = TfliteRunner::DelegateType::CPU;
    if(delegate == TfliteRunner::DelegateType::CPU) {
        delegateType = TfliteRunner::DelegateType::CPU;
    } else if(delegate == TfliteRunner::DelegateType::GPU) {
        delegateType = TfliteRunner::DelegateType::GPU;
    } else if(delegate == TfliteRunner::DelegateType::DSP) {
        delegateType = TfliteRunner::DelegateType::DSP;
    }
    rpc_server = std::make_unique<TfLiteRunnerRpcServer>(_address, delegateType);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeTfLiteRunnerServer_start(
        JNIEnv *env,
        jobject thiz) {
    rpc_server->Start();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeTfLiteRunnerServer_initAll(
        JNIEnv *env,
        jobject thiz,
        jstring root_address) {
    const char *cstr = env->GetStringUTFChars(root_address, NULL);
    std::string address(cstr);
    env->ReleaseStringUTFChars(root_address, cstr);

    tflite_cpu_rpc_server = std::make_unique<TfLiteRunnerRpcServer>(address+"/cpu", TfliteRunner::DelegateType::CPU);
    tflite_gpu_rpc_server = std::make_unique<TfLiteRunnerRpcServer>(address+"/gpu", TfliteRunner::DelegateType::GPU);
//    tflite_dsp_rpc_server = std::make_unique<TfLiteRunnerRpcServer>(, TfliteRunner::DelegateType::DSP);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_flomobility_anx_native_NativeTfLiteRunnerServer_startAll(
        JNIEnv *env,
        jobject thiz) {
    tflite_cpu_rpc_server->Start();
    tflite_gpu_rpc_server->Start();
}
