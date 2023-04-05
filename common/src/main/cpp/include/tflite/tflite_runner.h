//
// Created by Clay-Flo on 26/03/23.
//

#ifndef ANX_TFLITE_RUNNER_H
#define ANX_TFLITE_RUNNER_H

#include <vector>
#include <cstring>
#include <cstdlib>

#include <tensorflow/lite/c/c_api.h>
#include <tensorflow/lite/c/common.h>

#include <tensorflow/lite/delegates/gpu/delegate.h>
#include <tensorflow/lite/delegates/hexagon/hexagon_delegate.h>

class TfliteRunner {
public:
    enum DelegateType {
        CPU = 0,
        GPU = 1,
        DSP = 2
    };

    struct TensorMeta {
        std::vector<int> dims;
        int dtype;
    };

    struct ModelMeta {
        bool valid;
        std::vector<TensorMeta> input_tensors;
        std::vector<TensorMeta> output_tensors;
    };

    TfliteRunner();
    ~TfliteRunner();

    ModelMeta LoadModel(
            char *model_bytes,
            size_t model_size,
            DelegateType delegateType
    );

    std::vector<TfLiteTensor*> input_tensors_;
    std::vector<const TfLiteTensor*> output_tensors_;

    void InvokeModel();

private:
    bool model_loaded_;
    char *model_bytes_;
    size_t model_size_;
    DelegateType delegate_type_;

    TfLiteModel *model_;
    TfLiteDelegate *delegate_;
    TfLiteInterpreter *interpreter_;
    TfLiteInterpreterOptions *options_;
};

#endif //ANX_TFLITE_RUNNER_H
