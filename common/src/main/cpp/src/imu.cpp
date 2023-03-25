//
// Created by Clay-Flo on 25/03/23.
//

#include "sensor/imu.h"

#define TAG "NativeImu"


Imu::Imu(uint16_t fps, const std::string &address) : rate_(fps) {
    try {
        sensor_manager_ = std::make_unique<NdkSensorManager>();
        sensor_manager_->setListener(this);

        this->is_running_ = false;
        this->fps_ = fps;
        this->publisher = std::make_unique<Publisher>(address);

        LOGI(TAG, "Created IMU publisher on %s at a rate of %d hz", address.c_str(), fps);
    } catch (std::exception &e) {
        LOGE(TAG, "Error in creating IMU : %s", e.what());
    }
}

Imu::~Imu() {
    google::protobuf::ShutdownProtobufLibrary();
}

void Imu::start() {
    int32_t delay_in_u_secs = 1000000 / fps_;
    sensor_manager_->registerSensor(
            {ASENSOR_TYPE_ACCELEROMETER,
             ASENSOR_TYPE_GYROSCOPE,
             ASENSOR_TYPE_ROTATION_VECTOR,
             ASENSOR_TYPE_ACCELEROMETER_UNCALIBRATED,
             ASENSOR_TYPE_GYROSCOPE_UNCALIBRATED,
             ASENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED}, delay_in_u_secs);
    this->is_running_ = true;
    this->publisher_thread = std::make_unique<thread>(&Imu::publishData, this);
}

void Imu::onSensorChanged(ASensorEvent *event) {
    if (event->type == ASENSOR_TYPE_ACCELEROMETER) {
        this->data_.filtered.acceleration[0] = event->data[0];
        this->data_.filtered.acceleration[1] = event->data[1];
        this->data_.filtered.acceleration[2] = event->data[2];
    }

    if (event->type == ASENSOR_TYPE_GYROSCOPE) {
        this->data_.filtered.angular_velocity[0] = event->data[0];
        this->data_.filtered.angular_velocity[1] = event->data[1];
        this->data_.filtered.angular_velocity[2] = event->data[2];
    }

    if (event->type == ASENSOR_TYPE_ROTATION_VECTOR) {
        this->data_.filtered.orientation[0] = event->data[0];
        this->data_.filtered.orientation[1] = event->data[1];
        this->data_.filtered.orientation[2] = event->data[2];
        this->data_.filtered.orientation[3] = event->data[3];
    }

    if (event->type == ASENSOR_TYPE_ACCELEROMETER_UNCALIBRATED) {
        this->data_.raw.acceleration[0] = event->data[0];
        this->data_.raw.acceleration[1] = event->data[1];
        this->data_.raw.acceleration[2] = event->data[2];
    }

    if (event->type == ASENSOR_TYPE_GYROSCOPE_UNCALIBRATED) {
        this->data_.raw.angular_velocity[0] = event->data[0];
        this->data_.raw.angular_velocity[1] = event->data[1];
        this->data_.raw.angular_velocity[2] = event->data[2];
    }

    if (event->type == ASENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
        this->data_.raw.magnetic_field_u_tesla[0] = event->data[0];
        this->data_.raw.magnetic_field_u_tesla[1] = event->data[1];
        this->data_.raw.magnetic_field_u_tesla[2] = event->data[2];
    }
}

void Imu::stop() {
    sensor_manager_->unregister();
    this->is_running_ = false;
    this->publisher_thread->join();
}

void Imu::publishData() {
    while (is_running_) {
        try {
            // send data over zmq
            anx::ImuData data;
            data.mutable_raw()->mutable_acceleration()->set_x(
                    this->data_.raw.acceleration[0]);
            data.mutable_raw()->mutable_acceleration()->set_y(
                    this->data_.raw.acceleration[1]);
            data.mutable_raw()->mutable_acceleration()->set_z(
                    this->data_.raw.acceleration[2]);

            data.mutable_raw()->mutable_angular_velocity()->set_x(
                    this->data_.raw.angular_velocity[0]);
            data.mutable_raw()->mutable_angular_velocity()->set_y(
                    this->data_.raw.angular_velocity[1]);
            data.mutable_raw()->mutable_angular_velocity()->set_z(
                    this->data_.raw.angular_velocity[2]);

            data.mutable_raw()->mutable_magnetic_field_in_micro_tesla()->set_x(
                    this->data_.raw.magnetic_field_u_tesla[0]);
            data.mutable_raw()->mutable_magnetic_field_in_micro_tesla()->set_y(
                    this->data_.raw.magnetic_field_u_tesla[1]);
            data.mutable_raw()->mutable_magnetic_field_in_micro_tesla()->set_z(
                    this->data_.raw.magnetic_field_u_tesla[2]);

            data.mutable_filtered()->mutable_acceleration()->set_x(
                    this->data_.filtered.acceleration[0]);
            data.mutable_filtered()->mutable_acceleration()->set_y(
                    this->data_.filtered.acceleration[1]);
            data.mutable_filtered()->mutable_acceleration()->set_z(
                    this->data_.filtered.acceleration[2]);

            data.mutable_filtered()->mutable_angular_velocity()->set_x(
                    this->data_.filtered.angular_velocity[0]);
            data.mutable_filtered()->mutable_angular_velocity()->set_y(
                    this->data_.filtered.angular_velocity[1]);
            data.mutable_filtered()->mutable_angular_velocity()->set_z(
                    this->data_.filtered.angular_velocity[2]);

            data.mutable_filtered()->mutable_orientation()->set_x(
                    this->data_.filtered.orientation[0]);
            data.mutable_filtered()->mutable_orientation()->set_y(
                    this->data_.filtered.orientation[1]);
            data.mutable_filtered()->mutable_orientation()->set_z(
                    this->data_.filtered.orientation[2]);
            data.mutable_filtered()->mutable_orientation()->set_w(
                    this->data_.filtered.orientation[3]);

            std::string payload;
            data.SerializeToString(&payload);
            publisher->SendData(payload);
            rate_.sleep();
        } catch (std::exception &e) {
            LOGE(TAG, "Error in publishing : %s", e.what());
        }
    }
}
