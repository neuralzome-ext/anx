#include "opencv-utils.h"
#include <opencv2/imgproc.hpp>

void blur(cv::Mat& img, double sigma) {
    GaussianBlur(img, img, cv::Size(), sigma);
}

void bw(cv::Mat& img){
    cv::cvtColor(img, img, cv::COLOR_RGB2GRAY);
}

void resize(cv::Mat& img, int sizeX, int sizeY){
    cv::resize(img, img, cv::Size(sizeX, sizeY));
}