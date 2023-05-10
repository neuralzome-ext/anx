//
// Created by Clay-Flo on 03/03/23.
//

#include "ipc/ipc_transport.h"

Bytes::Bytes(size_t size, BYTE *data) {
    this->size_ = size;
    this->data_ = new BYTE[this->size_];
    memcpy(this->data_, data, this->size_);
}

Bytes::Bytes():
    size_(0), data_(nullptr) {}

Bytes::~Bytes() {
    delete this->data_;
}

BYTE *Bytes::bytes() {
    return this->data_;
}

size_t Bytes::size() {
    return this->size_;
}

void Bytes::set_bytes(size_t size, BYTE* data) {
    this->size_ = size;
    this->data_ = new BYTE[this->size_];
    memcpy(this->data_, data, this->size_);
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

void Publisher::SendData(const std::string &data) {
    SendData(data, false);
}

void Publisher::SendData(const std::string &data, bool wait) {
    try {
        zmq::send_flags flags = zmq::send_flags::dontwait;
        if(wait) flags = zmq::send_flags::none;
        this->socket_.send(
                zmq::message_t(data),
                flags);
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
Server::Server(const std::string &address) :
        bytes_(),
        has_message_(false),
        more_(false) {
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

bool Server::listen() {
    try {
        zmq::message_t msg;
        zmq::poll(this->poller_.get(), 1, 100);
        if (this->poller_->revents & ZMQ_POLLIN) {
            try {
                this->socket_->recv(msg);
            } catch (std::exception &e) {
                LOGE(this->tag_.c_str(), "Connection to %s terminated!", this->address_.c_str());
                return false;
            }
            this->bytes_.set_bytes(msg.size(), reinterpret_cast<BYTE *>(msg.data()));
            this->more_ = msg.more();
            return true;
        }
        this->more_ = false;
        return false;
    } catch (std::exception &e) {
        LOGE(this->tag_.c_str(), "Error in server listening %s", e.what());
        return false;
    }
}

Bytes Server::getData() {
    return this->bytes_;
}

bool Server::hasMessage() {
    return this->has_message_;
}

bool Server::hasMore() const {
    return this->more_;
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

bool Server::sendResponse(const std::string &payload) {
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

bool Server::sendResponse(const std::string &payload, bool more) {
    try {
        zmq::send_flags send_flags = zmq::send_flags::dontwait;
        if (more) {
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
