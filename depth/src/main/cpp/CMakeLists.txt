# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("depth")

message(STATUS "OPENCV_DIR=${OpenCV_DIR}")

set(OpenCV_STATIC on)
find_package(OpenCV REQUIRED)

# tflite
set(TFLITE_DIR ${TFLITE_ANDROID})#$ENV{TFLITE_ANDROID})
add_library(tflite-lib SHARED IMPORTED)
set_target_properties(tflite-lib
        PROPERTIES IMPORTED_LOCATION
        ${TFLITE_DIR}/jni/${ANDROID_ABI}/libtensorflowlite_jni.so)

# tflite-gpu
set(TFLITE_GPU_DIR ${TFLITE_GPU_ANDROID})#$ENV{TFLITE_GPU_ANDROID})
add_library(tflite-gpu-lib SHARED IMPORTED)
set_target_properties(tflite-gpu-lib
        PROPERTIES IMPORTED_LOCATION
        ${TFLITE_GPU_DIR}/jni/${ANDROID_ABI}/libtensorflowlite_gpu_jni.so)

STRING(REPLACE "-O0" "-O4" CMAKE_C_FLAGS_DEBUG ${CMAKE_C_FLAGS_DEBUG})
STRING(REPLACE "-O2" "-O4" CMAKE_C_FLAGS_RELEASE ${CMAKE_C_FLAGS_RELEASE})

add_library(depth SHARED depth.cpp)

add_library(
        pseudo-lidar
        SHARED
        midas/opencv-utils.cpp
        midas/midas.cpp)

include_directories(
        ${OpenCV_INCLUDE_DIRS}
        ${TFLITE_DIR}/headers
        ${TFLITE_GPU_DIR}/headers
)

target_include_directories(
        pseudo-lidar
        PRIVATE
        ${OpenCV_INCLUDE_DIRS}
        ${TFLITE_DIR}/headers
        ${TFLITE_GPU_DIR}/headers
)

find_library(
        android-lib
        android
)

find_library(
        jnigraphics-lib
        jnigraphics
)

target_link_libraries(
        pseudo-lidar
        tflite-lib
        tflite-gpu-lib
        ${OpenCV_LIBS}
        ${log-lib}
        ${android-lib}
        ${jnigraphics-lib})

find_library(log-lib log)

target_link_libraries(depth ${log-lib} ${OpenCV_LIBS} pseudo-lidar)
