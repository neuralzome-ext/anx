//
// Created by Clay-Flo on 03/03/23.
//

#ifndef ANX_UTILS_H
#define ANX_UTILS_H

#include <android/log.h>
#include <chrono>
#include <thread>

#define  LOGE(LOG_TAG, ...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(LOG_TAG, ...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(LOG_TAG, ...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(LOG_TAG, ...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

class Rate {
public:
    Rate(uint16_t hz);

    void sleep();

private:
    std::chrono::system_clock::time_point::duration start_;
    std::chrono::system_clock::time_point::duration expected_cycle_time_;
    std::chrono::system_clock::time_point::duration actual_cycle_time_;
};

#endif //ANX_UTILS_H
