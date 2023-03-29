//
// Created by Clay-Flo on 26/03/23.
//

#include "tflite/tflite_runner_rpc_server.h"

#define TAG "TfLiteRunnerRpcServer"

TfLiteRunnerRpcServer::TfLiteRunnerRpcServer(
        const std::string &address,
        TfliteRunner::DelegateType delegate):
        is_running_(false),
        socket_(ctx_, zmq::socket_type::rep) {
    this->delegate_ = delegate;
    this->socket_.bind(address);
    this->poll_.socket = this->socket_;
    this->poll_.fd = 0;
    this->poll_.events = ZMQ_POLLIN;
    this->poll_.revents = 0;
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
    LOGI(TAG, "TfLiteRunner RPC Server running");
    while(this->is_running_) {
        try {
            zmq::message_t req;
            zmq::poll(&this->poll_, 1, TIMEOUT_IN_MS);

            if (this->poll_.revents & ZMQ_POLLIN) {
                this->socket_.recv(&req);
                std::string rpc_name = req.to_string();

//                if (!req.more()) {
//                    std::stringstream ss;
//                    ss << "RPC " << rpc_name << "has no payload";
//                    LOGE(TAG, "%s", ss.str().c_str());
//
//                    anx::StdResponse res;
//                    res.set_success(false);
//                    res.set_message(ss.str());
//
//                    socket_.send(
//                            zmq::message_t(res.SerializeAsString()),
//                            zmq::send_flags::none);
//                    continue;
//                }

                LOGI(TAG, "Received rpc %s", rpc_name.c_str());

                if (rpc_name == LOAD_MODEL_RPC) {
                    zmq::message_t data_msg;

                    socket_.recv(&data_msg);
                    std::string payload_str = data_msg.to_string();

                    UnloadModel();

                    this->tflite_runners_.emplace_back();

                    anx::Payload payload;
                    payload.ParseFromString(payload_str);
                    this->tflite_runners_[0].LoadModel(
                            (char *)(payload.payload().data()),
                            payload.payload().size(),
                            this->delegate_);

                    anx::StdResponse res;
                    res.set_success(true);
                    socket_.send(
                            zmq::message_t(res.SerializeAsString()),
                            zmq::send_flags::sndmore);

                    anx::ModelMeta meta;
                    for (int i = 0; i < this->tflite_runners_[0].output_tensor_->dims->size; i++) {
                        meta.add_output_dims(this->tflite_runners_[0].output_tensor_->dims->data[i]);
                    }

                    for (int i = 0; i < this->tflite_runners_[0].input_tensor_->dims->size; i++) {
                        meta.add_input_dims(this->tflite_runners_[0].input_tensor_->dims->data[i]);
                    }

                    meta.set_input_dtype(this->tflite_runners_[0].input_tensor_->type);
                    meta.set_output_dtype(this->tflite_runners_[0].output_tensor_->type);
                    socket_.send(
                            zmq::message_t(meta.SerializeAsString()),
                            zmq::send_flags::none);
                    continue;
                }

                if (rpc_name == INVOKE_MODEL_RPC) {
                    try {
                        zmq::message_t data_msg;

                        socket_.recv(&data_msg);
                        std::string payload_str = data_msg.to_string();

                        anx::Payload payload;
                        payload.ParseFromString(payload_str);

                        // 1. Set Input tensor
                        memcpy(this->tflite_runners_[0].input_tensor_->data.data,
                               payload.payload().data(),
                               this->tflite_runners_[0].input_tensor_->bytes
                        );

                        // 2. Invoke Model
                        this->tflite_runners_[0].InvokeModel();

                        // 3. Get Output tensor
                        anx::Payload output_payload;
                        output_payload.set_payload(this->tflite_runners_[0].output_tensor_->data.data,
                                                   this->tflite_runners_[0].output_tensor_->bytes);

                        std::string rep_string;
                        output_payload.SerializeToString(&rep_string);
                        socket_.send(zmq::message_t(rep_string), zmq::send_flags::none);
                    } catch (std::exception &e) {
                        LOGE(TAG, "Error in Invoking model : %s", e.what());
                    }
                    continue;
                }

                if (rpc_name == UNLOAD_MODEL_RPC) {
                    zmq::message_t data_msg;

                    socket_.recv(&data_msg);

                    UnloadModel();

                    anx::StdResponse res;
                    res.set_success(true);
                    res.set_message("");
                    socket_.send(zmq::message_t(res.SerializeAsString()), zmq::send_flags::none);
                    continue;
                }

                // unknown RPC
                std::stringstream ss;
                ss << "Unknown RPC : " << rpc_name;
                std::string res_str;
                ss.str(res_str);
                LOGE(TAG, "%s", res_str.c_str());

                anx::StdResponse res;
                res.set_success(false);
                res.set_message(res_str.c_str(), res_str.size());

                std::string rep_string;
                res.SerializeToString(&rep_string);
                socket_.send(zmq::message_t(rep_string), zmq::send_flags::none);
            }
        } catch (std::exception &e) {
            LOGE(TAG, "Error in Loop : %s", e.what());
        }
    }
}

void TfLiteRunnerRpcServer::UnloadModel() {
    while(!this->tflite_runners_.empty()) {
        this->tflite_runners_.pop_back();
    }
}
