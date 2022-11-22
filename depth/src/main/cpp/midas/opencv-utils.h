#pragma once

#include <opencv2/core.hpp>

void blur(cv::Mat& img, double sigma);
void bw(cv::Mat& img);
void resize(cv::Mat& img, int sizeX, int sizeY);
