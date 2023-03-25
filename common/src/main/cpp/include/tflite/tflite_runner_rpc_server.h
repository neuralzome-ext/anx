//
// Created by Clay-Flo on 26/03/23.
//

#ifndef ANX_TFLITE_RUNNER_RPC_SERVER_H
#define ANX_TFLITE_RUNNER_RPC_SERVER_H

#include <thread>

#include "tflite_runner.h"
#include "model.pb.h"
#include "common.pb.h"
#include "ipc/ipc_transport.h"

#define LOAD_MODEL_RPC "LoadModel"
#define INVOKE_MODEL_RPC "InvokeModel"
#define UNLOAD_MODEL_RPC "UnloadModel"

class TfLiteRunnerRpcServer {
public:
    TfLiteRunnerRpcServer(
            const std::string &address,
            TfliteRunner::DelegateType delegate);
    ~TfLiteRunnerRpcServer();
    void Start();
private:
    void Loop();

    std::unique_ptr<Server> server_;
    std::unique_ptr<std::thread> server_thread_;
    std::unique_ptr<TfliteRunner> tflite_runner_;
    bool is_running_;
    TfliteRunner::DelegateType delegate_;
};

#endif //ANX_TFLITE_RUNNER_RPC_SERVER_H
