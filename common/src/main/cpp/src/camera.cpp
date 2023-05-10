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

AImageReader_ImageListener* NdkCamera::GetImageListener() {
    static AImageReader_ImageListener listener{
            .context = nullptr,
            .onImageAvailable = ImgCallback,
    };
    return &listener;
}

void NdkCamera::init() {

    auto id = GetBackFacingCamId();
    ACameraManager_openCamera(this->manager_, id.c_str(), &device_state_callbacks_, &device_);
    ACameraDevice_createCaptureRequest(device_,
                                       TEMPLATE_PREVIEW, &request_);

    ACaptureSessionOutputContainer_create(&output_container_);

    AImageReader_new(WIDTH, HEIGHT,
            /*format*/ AIMAGE_FORMAT_JPEG,
            /*max_images*/4,
                     &reader_);
    AImageReader_ImageListener listener{
            .context = this,
            .onImageAvailable = ImgCallback,
    };
    AImageReader_setImageListener(reader_, /*GetImageListener()*/&listener);

    AImageReader_getWindow(reader_, &window_);
    ANativeWindow_acquire(window_);
    ACameraOutputTarget_create(window_, &target_);
    ACaptureRequest_addTarget(request_, target_);

    ACaptureSessionOutput_create(window_, &output_);
    ACaptureSessionOutputContainer_add(output_container_, output_);

    ACameraDevice_createCaptureSession(device_, output_container_, &session_state_callbacks_,
                                       &session_);

}

bool NdkCamera::start() {
    if (is_running_) {
        stop();
    }
    ACameraCaptureSession_setRepeatingRequest(session_, &capture_callbacks,
            /*num_requests*/1, &request_,
            /*capture_sequence_id*/nullptr);
    is_running_ = true;
    return true;
}

void NdkCamera::stop() {
    if (!is_running_) return;
    ACameraCaptureSession_stopRepeating(session_);
    is_running_ = false;
}

void NdkCamera::ImageCallback(AImageReader *reader) {

    // send via zmq here
//    if (!is_running_) return;

    AImage *image = nullptr;
    auto status = AImageReader_acquireNextImage(reader, &image);

    if (status != AMEDIA_OK) return;

    uint8_t *data = nullptr;
    int len = 0;
    AImage_getPlaneData(image, 0, &data, &len);

    int32_t width, height;
    AImage_getWidth(image, &width);
    AImage_getHeight(image, &height);
    LOGD(TAG, "%d x %d", width, height);
    LOGD(TAG, "Size : %d", len);

    anx::CameraData cam_data;
    cam_data.set_image(std::string(data, data + len));

//    LOGD(TAG, "Length of image : %ld", cam_data.ByteSizeLong());
    std::string rep_string;
    cam_data.SerializeToString(&rep_string);
    this->publisher_->SendData(rep_string);
    AImage_delete(image);
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
