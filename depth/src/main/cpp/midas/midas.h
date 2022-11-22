#pragma once

#include<vector>

#include<opencv2/opencv.hpp>

#include<tensorflow/lite/c/c_api.h>
#include<tensorflow/lite/c/common.h>

class Midas {
public:
    Midas(const char *model_buffer, int size);

    ~Midas();

    cv::Mat forward(cv::Mat img);

private:
    const int INPUT_IMAGE_WIDTH = 256;
    const int INPUT_IMAGE_HEIGHT = 256;
    const std::vector<float> IMAGE_MEAN{0.485, 0.456, 0.406};
    const std::vector<float> IMAGE_STD{0.229, 0.224, 0.225};

    char *model_bytes = nullptr;
    TfLiteModel *model = nullptr;
    TfLiteInterpreter *interpreter = nullptr;
    TfLiteTensor *input_tensor = nullptr;
    const TfLiteTensor *output_tensor = nullptr;
};
