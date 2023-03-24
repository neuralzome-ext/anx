cmake_minimum_required(VERSION 3.18.1)

project("anx")
set(ANX_PROTO_DIR anx)

include_directories(
        ${PROTOBUF_DIR}/include
        ${ZMQ_DIR}/include
        include
        ${ANX_PROTO_DIR}
)

# get all .cc files from the anx directory
file(GLOB COMPILED_PROTO_FILES ${ANX_PROTO_DIR}/*.cc)

add_library( # Sets the name of the library.
        anx
        SHARED
        src/native-lib.cpp
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

target_link_libraries( # Specifies the target library.
        anx
        ipc_transport
        libprotobuf)
