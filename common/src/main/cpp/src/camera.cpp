//
// Created by Clay-Flo on 06/05/23.
//

#include "camera/camera.h"

#define TAG "NdkCamera"

NdkCamera::NdkCamera(const std::string &address)
        : manager_(nullptr),
          device_(nullptr),
          request_(nullptr),
          window_(nullptr),
          session_(nullptr),
          target_(nullptr),
          output_(nullptr),
          output_container_(nullptr),
          reader_(nullptr),
          is_running_(false) {
    this->manager_ = ACameraManager_create();
    this->publisher_ = std::make_unique<Publisher>(address);
}

NdkCamera::~NdkCamera() {
    // safely destroy objects
    ACameraCaptureSession_close(session_);
    ACaptureSessionOutputContainer_free(output_container_);
    ACaptureSessionOutput_free(output_);
    ACameraDevice_close(device_);
    ACameraManager_delete(manager_);
    AImageReader_delete(reader_);
    ACaptureRequest_free(request_);
    ANativeWindow_release(window_);

    output_container_ = nullptr;
    output_ = nullptr;
    device_ = nullptr;
    manager_ = nullptr;
    request_ = nullptr;
    window_ = nullptr;
    session_ = nullptr;
    target_ = nullptr;
}

void ImgCallback(void *context, AImageReader *reader) {
    std::thread img_callback_thread(
            &NdkCamera::ImageCallback,
            static_cast<NdkCamera*>(context),
            reader);
    img_callback_thread.detach();
}

anx::DeviceCameraSelect NdkCamera::GetAvailableStreams() {
    anx::DeviceCameraSelect selector;
    ACameraMetadata* metadataObj;
    ACameraManager_getCameraCharacteristics(this->manager_, this->camera_id_.c_str(), &metadataObj);

    ACameraMetadata_const_entry val = {0,};

    ACameraMetadata_getConstEntry(metadataObj, ACAMERA_SCALER_AVAILABLE_STREAM_CONFIGURATIONS, &val);
    for (uint32_t i = 0; i < val.count; i += 4) {
        // considering only MJPEG formats for now
        if(val.data.i32[i] == AIMAGE_FORMAT_JPEG) {
            anx::DeviceCameraStream* stream = selector.add_camera_streams();
            stream->set_width(val.data.i32[i + 1]);
            stream->set_height(val.data.i32[i + 2]);
            stream->set_fps(30);
            stream->set_pixel_format(anx::DeviceCameraStream::MJPEG);
        }
    }
    return selector;
}

void NdkCamera::init() {

    this->camera_id_ = GetBackFacingCamId();
    this->streams = GetAvailableStreams();
    ACaptureSessionOutputContainer_create(&output_container_);
    ACameraManager_openCamera(this->manager_, this->camera_id_.c_str(), &device_state_callbacks_, &device_);
}

bool NdkCamera::Start(const anx::StartDeviceCamera &stream) {
    if (is_running_) {
        stop();
    }

    // TODO add a stream checker
    ACameraDevice_createCaptureRequest(device_,
                                       TEMPLATE_PREVIEW, &request_);

    AImageReader_new(
            (int32_t) stream.camera_stream().width(),
            (int32_t) stream.camera_stream().height(),
            /*format*/ AIMAGE_FORMAT_JPEG,
            /*max_images*/4, &reader_);
    AImageReader_ImageListener listener{
            .context = this,
            .onImageAvailable = ImgCallback,
    };
    AImageReader_setImageListener(reader_, &listener);

    AImageReader_getWindow(reader_, &window_);
    ANativeWindow_acquire(window_);
    ACameraOutputTarget_create(window_, &target_);
    ACaptureRequest_addTarget(request_, target_);

    ACaptureSessionOutput_create(window_, &output_);
    ACaptureSessionOutputContainer_add(output_container_, output_);

    ACameraDevice_createCaptureSession(device_, output_container_, &session_state_callbacks_,
                                       &session_);

    ACameraCaptureSession_setRepeatingRequest(session_, &capture_callbacks,
            /*num_requests*/1, &request_,
            /*capture_sequence_id*/nullptr);
    is_running_ = true;
    return true;
}

void NdkCamera::stop() {
    if (!is_running_) return;
    ACameraCaptureSession_stopRepeating(session_);
    ACameraCaptureSession_close(session_);

    session_ = nullptr;

    ACaptureSessionOutputContainer_remove(output_container_, output_);
    ACaptureSessionOutput_free(output_);
    output_ = nullptr;

    ACameraOutputTarget_free(target_);
    target_ = nullptr;

    ANativeWindow_release(window_);
    window_ = nullptr;

    AImageReader_delete(reader_);
    reader_ = nullptr;

    ACaptureRequest_free(request_);
    request_ = nullptr;

    is_running_ = false;
}

void NdkCamera::ImageCallback(AImageReader *reader) {

    // send via zmq here
    if (!is_running_) return;

    AImage *image = nullptr;
    auto status = AImageReader_acquireNextImage(reader, &image);

    if (status != AMEDIA_OK) return;

    uint8_t *data = nullptr;
    int len = 0;
    AImage_getPlaneData(image, 0, &data, &len);

/*    int32_t width, height;
    AImage_getWidth(image, &width);
    AImage_getHeight(image, &height);
    LOGD(TAG, "%d x %d", width, height);
    LOGD(TAG, "Size : %d", len);*/

    anx::CameraData cam_data;
    cam_data.set_image(std::string(data, data + len));

//    LOGD(TAG, "Length of image : %ld", cam_data.ByteSizeLong());
    std::string rep_string;
    cam_data.SerializeToString(&rep_string);
    this->publisher_->SendData(rep_string);
    AImage_delete(image);
}

anx::StartDeviceCamera NdkCamera::Bytes2Stream(const std::string &data) {
    anx::StartDeviceCamera stream;
    stream.ParseFromString(data);
    return stream;
}

std::string NdkCamera::GetBackFacingCamId() {
    ACameraIdList *cameraIds = nullptr;
    ACameraManager_getCameraIdList(this->manager_, &cameraIds);

    std::string backId;

    LOGD(TAG, "found camera count %d", cameraIds->numCameras);

    for (int i = 0; i < cameraIds->numCameras; ++i) {
        const char *id = cameraIds->cameraIds[i];

        ACameraMetadata *metadataObj;
        ACameraManager_getCameraCharacteristics(this->manager_, id, &metadataObj);

        ACameraMetadata_const_entry lensInfo = {0};
        ACameraMetadata_getConstEntry(metadataObj, ACAMERA_LENS_FACING, &lensInfo);

        auto facing = static_cast<acamera_metadata_enum_android_lens_facing_t>(
                lensInfo.data.u8[0]);

        // Found a back-facing camera?
        if (facing == ACAMERA_LENS_FACING_BACK) {
            backId = id;
            break;
        }
    }

    ACameraManager_deleteCameraIdList(cameraIds);

    return backId;
}

// device state callbacks

void NdkCamera::OnDisconnected(void *context, ACameraDevice *device) {
    LOGI(TAG, "OnDisconnected()");
}

void NdkCamera::OnError(void *context, ACameraDevice *device, int error) {
    LOGI(TAG, "OnError()");
}

// session callbacks

void NdkCamera::OnSessionActive(void *context, ACameraCaptureSession *session) {
    LOGI(TAG, "OnSessionActive");
}

void NdkCamera::OnSessionReady(void *context, ACameraCaptureSession *session) {
    LOGI(TAG, "OnSessionReady");
}

void NdkCamera::OnSessionClosed(void *context, ACameraCaptureSession *session) {
    LOGI(TAG, "OnSessionClosed");
}

// capture callbacks
void NdkCamera::OnCaptureFailed(void *context, ACameraCaptureSession *session,
                                ACaptureRequest *request, ACameraCaptureFailure *failure) {
    LOGE(TAG, "OnCaptureFailed");
}

void NdkCamera::OnCaptureSequenceCompleted(void *context, ACameraCaptureSession *session,
                                           int sequenceId, int64_t frameNumber) {
    // TODO
}

void NdkCamera::OnCaptureSequenceAborted(void *context, ACameraCaptureSession *session,
                                         int sequenceId) {
    // TODO
}

void NdkCamera::OnCaptureCompleted(void *context, ACameraCaptureSession *session,
                                   ACaptureRequest *request, const ACameraMetadata *result) {
    // TODO
}
