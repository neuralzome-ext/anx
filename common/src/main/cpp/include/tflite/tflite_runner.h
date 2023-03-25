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

class TfliteRunner{
public:
    enum DelegateType{
        CPU = 0,
        GPU = 1,
        DSP = 2
    };

    struct ModelMeta{
        bool valid;
        std::vector<int> input_dims;
        std::vector<int> output_dims;
        int input_dtype;
        int output_dtype;
    };

    TfliteRunner();
    ~TfliteRunner();
    ModelMeta LoadModel(
            char* model_bytes,
            int model_size,
            DelegateType deligate_type
    );

    TfLiteTensor* input_tensor_;
    const TfLiteTensor* output_tensor_;
    void InvokeModel();
private:
    bool model_loaded_;
    char* model_bytes_;
    int model_size_;
    DelegateType delegate_type_;

    TfLiteModel* model_;
    TfLiteDelegate* delegate_;
    TfLiteInterpreter* interpreter_;
    TfLiteInterpreterOptions* options_;
};

#endif //ANX_TFLITE_RUNNER_H
