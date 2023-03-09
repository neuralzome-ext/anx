//
// Created by Clay-Flo on 03/03/23.
//

#include "ipc/ipc_transport.h"

Publisher::Publisher(const std::string &address)
        : socket_(context_, zmq::socket_type::pub) {
    try {
        this->tag_ = "NativeZmqPublisher[" + this->address_ + "]";

        this->address_ = address;
        this->socket_.bind(address);
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in initialization : %s", e.what());
    }
}

void Publisher::SendData(BYTE *data, int length) {
    try {
        this->socket_.send(
                zmq::message_t(data, length),
                zmq::send_flags::dontwait);
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in publishing data : %s", e.what());
    }
}

bool Publisher::close() {
    try {
        this->socket_.unbind(this->address_);
        this->socket_.close();
        return true;
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in closing publisher : %s", e.what());
        return false;
    }
}


// Subscriber related
Subscriber::Subscriber(
        const std::string &address,
        const std::string &topic) {
    try {
        this->tag_ = "NativeZmqSubscriber[" + this->address_ + "]";

        this->address_ = address;

        this->socket_ = std::make_unique<zmq::socket_t>(context_, zmq::socket_type::sub);
        this->socket_->connect(address);
        this->socket_->set(zmq::sockopt::subscribe, topic);

        this->poller_ = std::make_unique<zmq::pollitem_t>();
        this->poller_->socket = *this->socket_;
        this->poller_->fd = 0;
        this->poller_->events = ZMQ_POLLIN;
        this->poller_->revents = 0;

    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in initialization : %s", e.what());
    }
}

bytes_t Subscriber::listen() {
    bytes_t payload{};
    zmq::message_t msg;
    zmq::poll(this->poller_.get(), 1, 100);
    if (this->poller_->revents & ZMQ_POLLIN) {
        try {
            this->socket_->recv(msg);
        } catch (std::exception &e) {
            LOGI(this->tag_.c_str(), "Connection to %s terminated!", this->address_.c_str());
            return payload;
        }
        void *data_ptr = msg.data();
        BYTE *bytes = static_cast<BYTE *>(data_ptr);
        payload.data = bytes;
        payload.size = msg.size();
        return payload;
    }
    return payload;
}

bool Subscriber::close() {
    this->socket_->disconnect(this->address_);
    return true;
}

// Server related
Server::Server(const std::string &address) {
    this->socket_ = std::make_unique<zmq::socket_t>(this->context_, zmq::socket_type::rep);
    this->socket_->bind(address);

    this->poller_ = std::make_unique<zmq::pollitem_t>();
    this->poller_->socket = *this->socket_;
    this->poller_->fd = 0;
    this->poller_->events = ZMQ_POLLIN;
    this->poller_->revents = 0;
}

bytes_t Server::listen() {
    bytes_t payload{};
    zmq::message_t msg;
    zmq::poll(this->poller_.get(), 1, 100);
    if (this->poller_->revents & ZMQ_POLLIN) {
        try {
            this->socket_->recv(msg);
        } catch (std::exception &e) {
            LOGI(this->tag_.c_str(), "Connection to %s terminated!", this->address_.c_str());
            return payload;
        }
        void *data_ptr = msg.data();
        BYTE *bytes = static_cast<BYTE *>(data_ptr);
        payload.data = bytes;
        payload.size = msg.size();
        return payload;
    }
    return payload;
}

bool Server::close() {
    this->socket_->disconnect(this->address_);
    return true;
}
