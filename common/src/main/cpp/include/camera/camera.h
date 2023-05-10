//
// Created by Clay-Flo on 06/05/23.
//

#ifndef ANX_CAMERA_H
#define ANX_CAMERA_H

#include <string>
#include <fstream>
#include <vector>
#include <thread>
#include <memory>

#include <camera/NdkCameraManager.h>
#include <camera/NdkCameraMetadata.h>
#include <camera/NdkCameraDevice.h>
#include <media/NdkImageReader.h>
#include <android/native_window_jni.h>

#include "ipc/ipc_transport.h"
#include "assets.pb.h"
#include "common.pb.h"
#include "utils.h"

#define WIDTH 640
#define HEIGHT 480

class NdkCamera {
public:
    NdkCamera(const std::string &address);
    ~NdkCamera();
    void init();
    bool start();
    void stop();
    // image callback
    void ImageCallback(AImageReader* reader);
private:
    static void OnDisconnected(void *context, ACameraDevice *device);
    static void OnError(void *context, ACameraDevice *device, int error);

    // session callback functions
    static void OnSessionActive(void *context, ACameraCaptureSession *session);
    static void OnSessionReady(void *context, ACameraCaptureSession *session);
    static void OnSessionClosed(void *context, ACameraCaptureSession *session);

    AImageReader_ImageListener* GetImageListener();

    // capture callbacks
    static void OnCaptureFailed(void *context, ACameraCaptureSession *session,
                         ACaptureRequest *request, ACameraCaptureFailure *failure);
    static void OnCaptureSequenceCompleted(void *context, ACameraCaptureSession *session,
                                    int sequenceId, int64_t frameNumber);
    static void OnCaptureSequenceAborted(void *context, ACameraCaptureSession *session,
                                  int sequenceId);
    static void OnCaptureCompleted(void *context, ACameraCaptureSession *session,ACaptureRequest *request, const ACameraMetadata *result);


    std::string GetBackFacingCamId();

    bool is_running_;

    ACameraManager* manager_;
    ACameraDevice* device_;
    ACaptureRequest* request_;
    ANativeWindow* window_;
    ACameraCaptureSession* session_;
    ACameraOutputTarget* target_;
    ACaptureSessionOutput* output_;
    ACaptureSessionOutputContainer* output_container_;
    AImageReader* reader_;

    ACameraDevice_StateCallbacks device_state_callbacks_ = {
            .context = nullptr,
            .onDisconnected = OnDisconnected,
            .onError = OnError
    };

    ACameraCaptureSession_stateCallbacks session_state_callbacks_ = {
            .context = nullptr,
            .onActive = OnSessionActive,
            .onReady = OnSessionReady,
            .onClosed = OnSessionClosed
    };

    ACameraCaptureSession_captureCallbacks capture_callbacks{
            .context = nullptr,
            .onCaptureStarted = nullptr,
            .onCaptureProgressed = nullptr,
            .onCaptureCompleted = OnCaptureCompleted,
            .onCaptureFailed = OnCaptureFailed,
            .onCaptureSequenceCompleted = OnCaptureSequenceCompleted,
            .onCaptureSequenceAborted = OnCaptureSequenceAborted,
            .onCaptureBufferLost = nullptr,
    };

    // transport
    std::unique_ptr<Publisher> publisher_;

//    std::unique_ptr<uint8_t> data_;
//    int length_;
};

#endif //ANX_CAMERA_H
