//
// Created by Clay-Flo on 26/03/23.
//

#ifndef ANX_TFLITE_RUNNER_RPC_SERVER_H
#define ANX_TFLITE_RUNNER_RPC_SERVER_H

#include <thread>
#include <vector>

#include "tflite_runner.h"
#include "model.pb.h"
#include "common.pb.h"

#include "zmq/zmq.hpp"
#include "zmq/zmq_addon.hpp"

#include "utils.h"

#define LOAD_MODEL_RPC "LoadModel"
#define INVOKE_MODEL_RPC "InvokeModel"
#define UNLOAD_MODEL_RPC "UnloadModel"

#define TIMEOUT_IN_MS 100

class TfLiteRunnerRpcServer {
public:
    TfLiteRunnerRpcServer(
            const std::string &address,
            TfliteRunner::DelegateType delegate);
    ~TfLiteRunnerRpcServer();
    void Start();
private:
    void Loop();
    void UnloadModel();
    zmq::context_t ctx_;
    zmq::socket_t socket_;
    zmq::pollitem_t poll_;

    std::unique_ptr<std::thread> server_thread_;
    std::vector<TfliteRunner> tflite_runners_;

    bool is_running_;
    TfliteRunner::DelegateType delegate_;
};

#endif //ANX_TFLITE_RUNNER_RPC_SERVER_H
