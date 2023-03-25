//
// Created by Clay-Flo on 25/03/23.
//

#include "utils.h"

Rate::Rate(uint16_t hz)
        : start_(std::chrono::system_clock::now().time_since_epoch()),
          expected_cycle_time_(1000 / hz),
          actual_cycle_time_(0) {}

void Rate::sleep() {
    auto end = start_ + expected_cycle_time_;
    auto actual_end = std::chrono::system_clock::now().time_since_epoch();

    if (actual_end < start_) {
        end = actual_end + expected_cycle_time_;
    }

    auto sleep_duration = end - actual_end;
    actual_cycle_time_ = actual_end - start_;

    start_ = end;

    if (sleep_duration <= std::chrono::system_clock::duration(0)) {
        if(actual_end > end + expected_cycle_time_) {
            start_ = actual_end;
        }
        return;
    }
    std::this_thread::sleep_for(sleep_duration);
}
