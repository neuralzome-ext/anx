//
// Created by Clay-Flo on 03/03/23.
//

#include "ipc/ipc_transport.h"

Bytes::Bytes(size_t size, BYTE* data) {
    this->size_ = size;
    this->data_ = new BYTE [this->size_];
    memcpy(this->data_, data, this->size_);
}

Bytes::~Bytes() {
    delete this->data_;
}

BYTE* Bytes::bytes() {
    return this->data_;
}

size_t Bytes::size() {
    return this->size_;
}

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

void Publisher::SendData(bytes_t bytes) {
    try {
        this->socket_.send(
                zmq::message_t(bytes.data, bytes.size),
                zmq::send_flags::dontwait);
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in publishing data : %s", e.what());
    }
}

void Publisher::SendData(const std::string& data) {
    try {
        this->socket_.send(
                zmq::message_t(data),
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
    this->address_ = address;
    this->tag_ = "NativeZmqServer";

    this->socket_ = std::make_unique<zmq::socket_t>(this->context_, zmq::socket_type::rep);
    this->socket_->bind(address);

    LOGI(this->tag_.c_str(), "Created server on %s", this->address_.c_str());

    this->poller_ = std::make_unique<zmq::pollitem_t>();
    this->poller_->socket = *this->socket_;
    this->poller_->fd = 0;
    this->poller_->events = ZMQ_POLLIN;
    this->poller_->revents = 0;
}

seq_message_t Server::listen() {
    seq_message_t message{};
    bytes_t payload{};

    message.success = false;
    message.more = false;
    message.data = payload;

    zmq::message_t msg;
    zmq::poll(this->poller_.get(), 1, 100);
    if (this->poller_->revents & ZMQ_POLLIN) {
        try {
            this->socket_->recv(msg);
        } catch (std::exception &e) {
            LOGE(this->tag_.c_str(), "Connection to %s terminated!", this->address_.c_str());
            return message;
        }
        void* data_ptr = msg.data();

        BYTE *bytes = new BYTE[msg.size()];
        memcpy(bytes, data_ptr, msg.size());

        payload.size = msg.size();
        payload.data = bytes;

        message.success = true;
        message.data = payload;
        message.more = msg.more();
        return message;
    }
    return message;
}

rpc_payload_t Server::listenRpc() {
    rpc_payload_t payload;
    bytes_t data{};
    data.size = 0;
    data.data = {};
    payload.data = data;

    zmq::message_t msg1, msg2;

    std::string rpc_name;

    zmq::poll(this->poller_.get(), 1, 100);
    if (this->poller_->revents & ZMQ_POLLIN) {
        try {
            auto res = this->socket_->recv(msg1);
            rpc_name = msg1.to_string();
            if(!msg1.more()) {
                LOGE(this->tag_.c_str(), "Invalid RPC : %s", rpc_name.c_str());
                return payload;
            }
            auto res2 = this->socket_->recv(msg2);
            void *data_ptr = msg2.data();
            BYTE *bytes = static_cast<BYTE *>(data_ptr);
            data.data = bytes;
            data.size = msg2.size();

            payload.data = data;
            payload.rpc_name = rpc_name;

            return payload;
        } catch (std::exception &e) {
            LOGE(this->tag_.c_str(), "Connection to %s terminated!", this->address_.c_str());
            return payload;
        }
    }
    return payload;
}

bool Server::sendResponse(bytes_t &payload) {
    try {
        this->socket_->send(
                zmq::message_t(payload.data, payload.size),
                zmq::send_flags::dontwait);
        return true;
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in sending response data : %s", e.what());
        return false;
    }
}

bool Server::sendResponse(const std::string& payload) {
    try {
        this->socket_->send(
                zmq::message_t(payload),
                zmq::send_flags::dontwait);
        return true;
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in sending response data : %s", e.what());
        return false;
    }
}

bool Server::sendResponse(const std::string& payload, bool more) {
    try {
        zmq::send_flags send_flags = zmq::send_flags::dontwait;
        if(more) {
            send_flags = zmq::send_flags::sndmore;
        }
        this->socket_->send(
                zmq::message_t(payload),
                send_flags);
        return true;
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in sending response data : %s", e.what());
        return false;
    }
}

bool Server::close() {
    this->socket_->disconnect(this->address_);
    this->socket_->close();
    this->context_.close();
    return true;
}
