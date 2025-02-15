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
                    TfliteRunner::ModelMeta model_meta = this->tflite_runners_[0].LoadModel(
                            (char *)(payload.payload().data()),
                            payload.payload().size(),
                            this->delegate_);

                    anx::StdResponse res;
                    res.set_success(true);
                    socket_.send(
                            zmq::message_t(res.SerializeAsString()),
                            zmq::send_flags::sndmore);

                    anx::ModelMeta meta;
                    // populate input meta
                    for(auto & input_tensor : model_meta.input_tensors) {
                        anx::TensorMeta* tensor_meta = meta.add_input_tensors();

                        tensor_meta->set_dtype(input_tensor.dtype);
                        for(int dim : input_tensor.dims) {
                            tensor_meta->add_dims(dim);
                        }
                    }

                    // populate output meta
                    for(auto & output_tensor : model_meta.output_tensors) {
                        anx::TensorMeta* tensor_meta = meta.add_output_tensors();

                        tensor_meta->set_dtype(output_tensor.dtype);
                        for(int dim : output_tensor.dims) {
                            tensor_meta->add_dims(dim);
                        }
                    }

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

                        anx::PayloadArray payload;
                        payload.ParseFromString(payload_str);

                        if(payload.payloads_size() != this->tflite_runners_[0].input_tensors_.size()) {
                            LOGE(TAG, "Received %d input tensors, but expected %ld", payload.payloads_size(), this->tflite_runners_[0].input_tensors_.size());
                            continue;
                        }

                        for(int i = 0; i < payload.payloads_size(); i++) {
                            // 1. Set Input tensor
                            memcpy(this->tflite_runners_[0].input_tensors_[i]->data.data,
                                   payload.payloads(i).data(),
                                   this->tflite_runners_[0].input_tensors_[i]->bytes
                            );
                        }

                        // 2. Invoke Model
                        this->tflite_runners_[0].InvokeModel();

                        // 3. Get Output tensor
                        anx::PayloadArray output_payload;
                        for(auto & output_tensor : this->tflite_runners_[0].output_tensors_) {
                            // populate the payload
                            output_payload.add_payloads(
                                    output_tensor->data.data,
                                    output_tensor->bytes);
                        }

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
