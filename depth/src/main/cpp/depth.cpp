#include <jni.h>
#include <string>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "android/bitmap.h"
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include "midas/opencv-utils.h"
#include "midas/midas.h"
#include <cmath>
#include <fstream>
#include <jni.h>

void bitmapToMat(JNIEnv *env, jobject bitmap, cv::Mat &dst, jboolean needUnPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void *pixels = 0;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        dst.create(info.height, info.width, CV_8UC4);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (needUnPremultiplyAlpha) cvtColor(tmp, dst, cv::COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, cv::COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch (const cv::Exception &e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}

void matToBitmap(JNIEnv *env, cv::Mat src, jobject bitmap, jboolean needPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void *pixels = 0;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(src.dims == 2 && info.height == (uint32_t) src.rows &&
                  info.width == (uint32_t) src.cols);
        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, cv::COLOR_GRAY2RGBA);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, cv::COLOR_RGB2RGBA);
            } else if (src.type() == CV_8UC4) {
                if (needPremultiplyAlpha) cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if (src.type() == CV_8UC1) {
                cvtColor(src, tmp, cv::COLOR_GRAY2BGR565);
            } else if (src.type() == CV_8UC3) {
                cvtColor(src, tmp, cv::COLOR_RGB2BGR565);
            } else if (src.type() == CV_8UC4) {
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch (const cv::Exception &e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_flomobility_depth_NativeLib_blur(
        JNIEnv *env,
        jobject /* this */,
        jobject bitmapIn,
        jobject bitmapOut,
        jdouble sigma) {
    cv::Mat img;
    bitmapToMat(env, bitmapIn, img, false);
    blur(img, sigma);
    matToBitmap(env, img, bitmapOut, false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_flomobility_depth_NativeLib_bw(
        JNIEnv *env,
        jobject /* this */,
        jobject bitmapIn,
        jobject bitmapOut) {
    cv::Mat img;
    bitmapToMat(env, bitmapIn, img, false);
    bw(img);
    matToBitmap(env, img, bitmapOut, false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_flomobility_depth_NativeLib_resize(
        JNIEnv *env,
        jobject /* this */,
        jobject bitmapIn,
        jobject bitmapOut,
        jint sizeX,
        jint sizeY) {
    cv::Mat img;
    bitmapToMat(env, bitmapIn, img, false);
    resize(img, sizeX, sizeY);
    matToBitmap(env, img, bitmapOut, false);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_flomobility_depth_NativeLib_initMidas(
        JNIEnv *env,
        jobject p_this,
        jobject assetManager,
        jstring modelName) {

    char *buffer = nullptr;
    long size = 0;

    const char *model_path = env->GetStringUTFChars(modelName, NULL);

    if (!(env->IsSameObject(assetManager, NULL))) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        AAsset *asset = AAssetManager_open(mgr, model_path, AASSET_MODE_UNKNOWN);
        assert(asset != nullptr);

        size = AAsset_getLength(asset);
        buffer = (char *) malloc(sizeof(char) * size);
        AAsset_read(asset, buffer, size);
        AAsset_close(asset);
    }

    auto *midas = new Midas(buffer, size);
    if (strcmp(model_path, "model_midas.tflite") == 0) {
        midas->INPUT_IMAGE_WIDTH = 256;
        midas->INPUT_IMAGE_HEIGHT = 256;
    } else if (strcmp(model_path, "model_packnet.tflite") == 0) {
        midas->INPUT_IMAGE_WIDTH = 640;
        midas->INPUT_IMAGE_HEIGHT = 192;
    }
    jlong res = (jlong) midas;
    free(buffer); // Midas duplicate it
    return res;

/*    if (!(env->IsSameObject(assetManager, NULL))) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        AAsset *asset = AAssetManager_open(mgr, model_name, AASSET_MODE_UNKNOWN);
        assert(asset != nullptr);

        size = AAsset_getLength(asset);
        buffer = (char *) malloc(sizeof(char) * size);
        AAsset_read(asset, buffer, size);
        AAsset_close(asset);
    }*/

//    std::fstream file;
//    file.open(model_path);

/*    FILE *file;
    file = fopen(model_path, "r");
    fseek(file, 0, SEEK_END);
    size = ftell(file);

    buffer = (char *) malloc(sizeof(char) * size);
    fseek(file, 0, SEEK_SET);

    fread(buffer, size, 1, file);
    jlong res = (jlong) new Midas(buffer, size);
    free(buffer); // Midas duplicate it
    fclose(file);
    file = nullptr;
    return res;*/
}

extern "C" JNIEXPORT void JNICALL
Java_com_flomobility_depth_NativeLib_destroyMidas(
        JNIEnv *env,
        jobject p_this,
        jlong midas_ptr) {
    if (midas_ptr)
        delete (Midas *) midas_ptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_flomobility_depth_NativeLib_depthMidas(
        JNIEnv *env,
        jobject p_this,
        jlong midas_ptr,
        jobject bitmapIn,
        jobject bitmapOut) {
    cv::Mat input_img;
    bitmapToMat(env, bitmapIn, input_img, false);

    Midas *midas = (Midas *) midas_ptr;
    cv::Mat output_image = midas->forward(input_img);
    matToBitmap(env, output_image, bitmapOut, false);
}
