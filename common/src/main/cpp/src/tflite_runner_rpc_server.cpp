//
// Created by Clay-Flo on 26/03/23.
//

#include "tflite/tflite_runner_rpc_server.h"

#define TAG "TfLiteRunnerRpcServer"

TfLiteRunnerRpcServer::TfLiteRunnerRpcServer(
        const std::string &address,
        TfliteRunner::DelegateType delegate): is_running_(false) {
    this->server_ = std::make_unique<Server>(address);
    this->tflite_runner_ = std::make_unique<TfliteRunner>();
    this->delegate_ = delegate;
}

TfLiteRunnerRpcServer::~TfLiteRunnerRpcServer() {
    this->is_running_ = false;
    this->server_thread_->join();
}

void TfLiteRunnerRpcServer::Start() {
    this->is_running_ = true;
    this->server_thread_ = std::make_unique<std::thread>(&TfLiteRunnerRpcServer::Loop, this);
}

void TfLiteRunnerRpcServer::Loop() {
    while(this->is_running_) {
        seq_message_t msg = this->server_->listen();
        if(!msg.success) continue;

        std::string rpc(reinterpret_cast<char *>(msg.data.data), msg.data.size);
        if(!msg.more) {
            std::stringstream ss;
            ss << "RPC " << rpc << "has no payload";
            LOGE(TAG, "%s", ss.str().c_str());

            anx::StdResponse res;
            res.set_success(false);
            res.set_message(ss.str());
            this->server_->sendResponse(res.SerializeAsString());
            continue;
        }

        seq_message_t data_msg = this->server_->listen();
        std::string payload_bytes(reinterpret_cast<char *>(data_msg.data.data), data_msg.data.size);

        if(rpc == LOAD_MODEL_RPC) {
            anx::Payload payload;
            payload.ParseFromString(payload_bytes);
            this->tflite_runner_->LoadModel(
                    const_cast<char *>(payload.payload().c_str()),
                    payload.payload().size(),
                    this->delegate_);
            continue;
        }

        if (rpc == INVOKE_MODEL_RPC) {
            anx::Payload payload;
            payload.ParseFromString(payload_bytes);

            // 1. Set Input tensor
            memcpy(this->tflite_runner_->input_tensor_->data.data,
                   payload.payload().c_str(),
                   this->tflite_runner_->input_tensor_->bytes
            );

            // 2. Invoke Model
            this->tflite_runner_->InvokeModel();

            // 3. Get Output tensor
            anx::Payload output_payload;
            output_payload.set_payload(this->tflite_runner_->output_tensor_->data.data, this->tflite_runner_->output_tensor_->bytes);

            std::string rep_string;
            output_payload.SerializeToString(&rep_string);
            this->server_->sendResponse(rep_string);
            continue;
        }

        if (rpc == UNLOAD_MODEL_RPC) {
            anx::StdResponse res;
            res.set_success(true);
            res.set_message("");
            this->server_->sendResponse(res.SerializeAsString());
            continue;
        }

    }
}
