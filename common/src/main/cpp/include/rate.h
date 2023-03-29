//
// Created by Clay-Flo on 28/03/23.
//

#ifndef ANX_RATE_H
#define ANX_RATE_H

#include <chrono>
#include <thread>

class Rate {
public:
    Rate(uint16_t hz);

    void sleep();

private:
    uint64_t get_stamp_in_ms();
    uint64_t start_;
    uint64_t expected_cycle_time_;
    uint64_t actual_cycle_time_;
};

#endif //ANX_RATE_H
