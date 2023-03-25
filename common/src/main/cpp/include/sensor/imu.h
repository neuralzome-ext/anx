//
// Created by Clay-Flo on 25/03/23.
//

#ifndef ANX_IMU_H
#define ANX_IMU_H

#include <thread>
#include <exception>
#include <chrono>

#include "NdkSensor.h"
#include "ipc/ipc_transport.h"
#include "assets.pb.h"
#include "utils.h"

struct imu_raw_t {
    double acceleration[3];
    double angular_velocity[3];
    double magnetic_field_u_tesla[3];
};

struct imu_filtered_t {
    double acceleration[3];
    double angular_velocity[3];
    double orientation[4];
};

struct imu_data_t {
    imu_raw_t raw;
    imu_filtered_t filtered;
};

class Imu: public NdkSensorEventListener {
public:
    Imu(uint16_t fps, const std::string& address);
    ~Imu();
    void start();
    void stop();
private:
    void onSensorChanged(ASensorEvent *event) override;
    void publishData();
    std::unique_ptr<NdkSensorManager> sensor_manager_;
    std::unique_ptr<thread> publisher_thread;
    std::unique_ptr<Publisher> publisher;

    bool is_running_;
    Rate rate_;
    imu_data_t data_{};
    uint16_t fps_;
};

#endif //ANX_IMU_H
