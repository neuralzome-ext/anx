//
// Created by Clay-Flo on 28/03/23.
//

#include "rate.h"

Rate::Rate(uint16_t hz)
        : start_(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count()),
          expected_cycle_time_(1000 / hz),
          actual_cycle_time_(0L) {}

uint64_t Rate::get_stamp_in_ms() {
    auto stamp = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    return stamp;
}

void Rate::sleep() {
    auto end = start_ + expected_cycle_time_;
    auto actual_end = get_stamp_in_ms();

    if (actual_end < start_) {
        end = actual_end + expected_cycle_time_;
    }

    auto sleep_duration = end - actual_end;
    actual_cycle_time_ = actual_end - start_;

    start_ = end;

    if (sleep_duration <= 0L) {
        if(actual_end > end + expected_cycle_time_) {
            start_ = actual_end;
        }
        return;
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(sleep_duration));
}
