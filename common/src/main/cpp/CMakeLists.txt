cmake_minimum_required(VERSION 3.18.1)

project("anx")
set(ANX_PROTO_DIR anx)

include_directories(
        ${PROTOBUF_DIR}/include
        ${ZMQ_DIR}/include
        ${TFLITE_DIR}/include
        ${TFLITE_GPU_DIR}/include
        ${TFLITE_HEXAGON_DIR}/include
        include
        ${ANX_PROTO_DIR}
)

# get all .cc files from the anx directory
file(GLOB COMPILED_PROTO_FILES ${ANX_PROTO_DIR}/*.cc)

add_library( # Sets the name of the library.
        anx
        SHARED
        src/native-lib.cpp
        src/sensord.cpp
        src/imu.cpp
        src/NdkSensor.cpp
        src/utils.cpp
        ${COMPILED_PROTO_FILES})

add_library(
        libzmq
        SHARED
        IMPORTED
)
set_target_properties(libzmq
        PROPERTIES
        IMPORTED_LOCATION
        ${ZMQ_DIR}/libzmq.so)

find_library(
        log-lib
        log)

add_library(
        cppzmq
        INTERFACE
)
target_include_directories(
        cppzmq
        INTERFACE
        ${ZMQ_DIR}/include
)

target_link_libraries(
        cppzmq
        INTERFACE
        libzmq
)

add_library(
        libprotobuf
        STATIC
        IMPORTED
)
set_target_properties(libprotobuf
        PROPERTIES
        IMPORTED_LOCATION
        ${PROTOBUF_DIR}/libprotobuf.a)

add_library(
        ipc_transport
        SHARED
        src/ipc_transport.cpp
)

target_include_directories(
        ipc_transport
        INTERFACE
        include
)

target_link_libraries(
        ipc_transport
        cppzmq
        ${log-lib}
        libprotobuf
)

add_library(
        libtflite
        SHARED
        IMPORTED)

set_target_properties(libtflite
        PROPERTIES
        IMPORTED_LOCATION
        ${TFLITE_DIR}/libtensorflowlite_jni.so)

add_library(
        libtflite_gpu
        SHARED
        IMPORTED)

set_target_properties(libtflite_gpu
        PROPERTIES
        IMPORTED_LOCATION
        ${TFLITE_GPU_DIR}/libtensorflowlite_gpu_jni.so)

add_library(libhexagon_interface
        SHARED
        IMPORTED)

set_target_properties(libhexagon_interface
        PROPERTIES
        IMPORTED_LOCATION
        ${TFLITE_HEXAGON_DIR}/libhexagon_interface.so)

add_library(
        libtflite_hexagon
        SHARED
        IMPORTED)

set_target_properties(libtflite_hexagon
        PROPERTIES
        IMPORTED_LOCATION
        ${TFLITE_HEXAGON_DIR}/libtensorflowlite_hexagon_jni.so)

target_link_libraries(libtflite_hexagon INTERFACE libhexagon_interface)

add_library(anx_tflite_runner
        SHARED
        src/tflite_runner.cpp
        src/tflite_runner_rpc_server.cpp
        src/tflite_runner_jni.cpp
        ${COMPILED_PROTO_FILES})

target_link_libraries(
        anx_tflite_runner
        ipc_transport
        libprotobuf
        libtflite
        libtflite_gpu
        libtflite_hexagon
)

target_link_libraries( # Specifies the target library.
        anx
        ipc_transport
        libprotobuf
        android)
