//
// Created by Clay-Flo on 26/03/23.
//

#include "tflite/tflite_runner.h"

TfliteRunner::TfliteRunner(){
    this->model_loaded_ = false;
    this->model_bytes_ = nullptr;
    this->model_size_ = 0L;
    this->delegate_type_ = TfliteRunner::DelegateType::CPU;

    this->model_ = nullptr;
    this->interpreter_ = nullptr;
    this->options_ = nullptr;
    this->input_tensor_ = nullptr;
    this->output_tensor_ = nullptr;
}

TfliteRunner::~TfliteRunner(){
    free(this->model_bytes_);
    TfLiteInterpreterDelete(this->interpreter_);
    TfLiteInterpreterOptionsDelete(this->options_);
    TfLiteModelDelete(this->model_);

    if(this->delegate_type_ == TfliteRunner::DelegateType::GPU){
        TfLiteGpuDelegateV2Delete(this->delegate_);
    }else if(this->delegate_type_ == TfliteRunner::DelegateType::DSP){
        TfLiteHexagonDelegateDelete(this->delegate_);
    }
}

TfliteRunner::ModelMeta TfliteRunner::LoadModel(
        char* model_bytes,
        size_t model_size,
        DelegateType delegateType
){
    TfliteRunner::ModelMeta model_meta;
    this->delegate_type_ = delegateType;

    this->model_size_ = model_size;
    this->model_bytes_ = (char*) malloc(this->model_size_);
    memcpy(this->model_bytes_, model_bytes, this->model_size_);

    this->model_ = TfLiteModelCreate(this->model_bytes_, this->model_size_);

    this->options_ = TfLiteInterpreterOptionsCreate();

    if(this->delegate_type_ == TfliteRunner::DelegateType::GPU){
        const TfLiteGpuDelegateOptionsV2 gpu_delegate_options = {
                .inference_preference = TFLITE_GPU_INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER,
                .is_precision_loss_allowed = 1, // FP16
                .max_delegated_partitions = 4
        };
        this->delegate_ = TfLiteGpuDelegateV2Create(&gpu_delegate_options);

        TfLiteInterpreterOptionsAddDelegate(this->options_, this->delegate_);
    }else if(this->delegate_type_ == TfliteRunner::DelegateType::DSP){
        const TfLiteHexagonDelegateOptions hexagon_delegate_options = {
                .debug_level = 0,
                .powersave_level = 0
        };
        this->delegate_ = TfLiteHexagonDelegateCreate(&hexagon_delegate_options);
    }else{
        TfLiteInterpreterOptionsSetNumThreads(this->options_, 8);
    }

    this->interpreter_ = TfLiteInterpreterCreate(this->model_, this->options_);

    if (TfLiteInterpreterAllocateTensors(this->interpreter_) != kTfLiteOk) {
        model_meta.valid = false;
        return model_meta;
    }

    this->input_tensor_ = TfLiteInterpreterGetInputTensor(this->interpreter_, 0);
    this->output_tensor_ = TfLiteInterpreterGetOutputTensor(this->interpreter_, 0);

    // Populate model_meta
    model_meta.valid = true;

    for(int i=0; i<this->input_tensor_->dims->size; i++){
        model_meta.input_dims.emplace_back(this->input_tensor_->dims->data[i]);
    }

    for(int i=0; i<this->output_tensor_->dims->size; i++){
        model_meta.output_dims.emplace_back(this->output_tensor_->dims->data[i]);
    }

    model_meta.input_dtype = this->input_tensor_->type;

    model_meta.output_dtype = this->output_tensor_->type;

    return model_meta;
}

void TfliteRunner::InvokeModel(){
    TfLiteInterpreterInvoke(this->interpreter_);
}
