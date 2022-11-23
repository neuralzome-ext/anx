#include"midas.h"

#include<tensorflow/lite/delegates/gpu/delegate.h>

Midas::Midas(const char* model_buffer, int size){
    model_bytes = (char*)malloc(sizeof(char) * size);
    memcpy(model_bytes, model_buffer, sizeof(char) * size);
    model = TfLiteModelCreate(model_bytes, size);

	TfLiteInterpreterOptions* options = TfLiteInterpreterOptionsCreate();

    const TfLiteGpuDelegateOptionsV2 delegate_options = {
            .inference_preference = TFLITE_GPU_INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER,
            .is_precision_loss_allowed = 1, // FP16
            .max_delegated_partitions = 4
    };
    TfLiteDelegate* delegate = TfLiteGpuDelegateV2Create(&delegate_options);
    TfLiteInterpreterOptionsAddDelegate(options, delegate);

//	TfLiteInterpreterOptionsSetNumThreads(options, 4);

	interpreter = TfLiteInterpreterCreate(model, options);

	if (TfLiteInterpreterAllocateTensors(interpreter) != kTfLiteOk) {
		printf("Failed to allocate tensors!");
		return;
	}

    input_tensor = TfLiteInterpreterGetInputTensor(interpreter, 0);
    output_tensor = TfLiteInterpreterGetOutputTensor(interpreter, 0);
}

Midas::~Midas(){
    TfLiteModelDelete(model);
}

cv::Mat Midas::forward(cv::Mat input_img){
    cv::Mat img;

    cv::resize(input_img, img, cv::Size(INPUT_IMAGE_WIDTH , INPUT_IMAGE_HEIGHT), 0, 0, cv::INTER_CUBIC);

    cvtColor(img, img, cv::COLOR_BGR2RGB);

    // Normalize Image
    cv::Mat fimg;
    img.convertTo(fimg, CV_32FC3, 1.0/ 255.0, 0.0);

    /* for(int r = 0; r < fimg.rows; r++) { */
    /*     cv::Vec3b* ptr = fimg.ptr<cv::Vec3b>(r); */
    /*     for(int c = 0; c < fimg.cols; c++) { */
    /*         ptr[c] = cv::Vec3b( */
				/* (ptr[c][0] - IMAGE_MEAN[0])/IMAGE_STD[0], */
				/* (ptr[c][1] - IMAGE_MEAN[1])/IMAGE_STD[1], */
				/* (ptr[c][2] - IMAGE_MEAN[2])/IMAGE_STD[2] */
			/* ); */
    /*     } */
    /* } */

    // Copy Image to input_tensor
    memcpy(input_tensor->data.f, fimg.data,
        sizeof(float) * INPUT_IMAGE_WIDTH * INPUT_IMAGE_HEIGHT * 3
    );
    

    // Invoke interpreter
    TfLiteInterpreterInvoke(interpreter);

    // Make sense of output_tensor
    cv::Mat output_fimg_scaled = cv::Mat(
        output_tensor->dims->data[1],
        output_tensor->dims->data[2],
        CV_32FC1,
        output_tensor->data.f
    );

    cv::Mat output_img_scaled, output_img;
    cv::normalize(output_fimg_scaled, output_img_scaled,
                0, 255, cv::NORM_MINMAX, CV_8UC1);

    resize(output_img_scaled, output_img,
        cv::Size(input_img.cols, input_img.rows),
        0, 0, cv::INTER_CUBIC);

    applyColorMap(output_img, output_img, cv::COLORMAP_MAGMA);
    cvtColor(output_img, output_img, cv::COLOR_RGB2BGR);
    return output_img;
}

