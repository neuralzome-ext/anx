//
// Created by Clay-Flo on 03/03/23.
//

#ifndef ANX_IPC_TRANSPORT_H
#define ANX_IPC_TRANSPORT_H

#include <string>
#include <memory>
#include <algorithm>
#include <exception>

#include "zmq/zmq.hpp"
#include "zmq/zmq_addon.hpp"

#include "utils.h"

typedef unsigned char BYTE;   // 8-bit unsigned entity.

class Bytes {
public:
    Bytes(size_t, BYTE*);
    ~Bytes();
    BYTE* bytes();
    size_t size();
private:
    BYTE* data_;
    size_t size_;
};

struct bytes_t {
    BYTE* data;
    size_t size;
};

struct rpc_payload_t {
    std::string rpc_name;
    bytes_t data;
};

struct seq_message_t {
    bool success;
    bytes_t data;
    bool more;
};

class Publisher {
public:
    Publisher(const std::string& address);
    void SendData(BYTE* data, int length);
    void SendData(bytes_t bytes);
    void SendData(const std::string& data);
    bool close();
private:
    zmq::context_t context_;
    zmq::socket_t socket_;

    std::string address_;

    std::string tag_;
};

class Subscriber {
public:
    Subscriber(const std::string& address, const std::string& topic);
    bytes_t listen();
    bool close();
private:
    zmq::context_t context_;
    std::unique_ptr<zmq::socket_t> socket_;
    std::unique_ptr<zmq::pollitem_t> poller_;

    std::string address_;

    std::string tag_;
};

class Server {
public:
    Server(const std::string& address);
    seq_message_t listen();
    rpc_payload_t listenRpc();
    bool sendResponse(bytes_t& payload);
    bool sendResponse(const std::string& payload);
    bool sendResponse(const std::string& payload, bool more);
    bool close();
private:
    zmq::context_t context_;
    std::unique_ptr<zmq::socket_t> socket_;
    std::unique_ptr<zmq::pollitem_t> poller_;

    std::string address_;

    std::string tag_;
};

class Client {

};

#endif //ANX_IPC_TRANSPORT_H
