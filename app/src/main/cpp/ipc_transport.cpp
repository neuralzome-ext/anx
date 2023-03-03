//
// Created by Clay-Flo on 03/03/23.
//

#include "ipc_transport.h"

Publisher::Publisher(const std::string& address)
        : socket_(context_, zmq::socket_type::pub) {
    try {
        this->tag_ = "NativeZmqPublisher[" + this->address_+"]";

        this->address_ = address;
        this->socket_.bind(address);
    } catch (std::exception& e) {
        LOGE(this->tag_.c_str(), "Error in initialization : %s", e.what());
    }
}

void Publisher::SendData(BYTE* data, int length) {
    try {
        this->socket_.send(
                zmq::message_t(data, length),
                zmq::send_flags::dontwait);
    } catch(std::exception& e) {
        LOGE(this->tag_.c_str(), "Error in publishing data : %s", e.what());
    }
}

bool Publisher::close() {
    try {
        this->socket_.unbind(this->address_);
        this->socket_.close();
        return true;
    } catch (std::exception& e) {
        LOGE(this->tag_.c_str(), "Error in closing publisher : %s", e.what());
        return false;
    }
}
