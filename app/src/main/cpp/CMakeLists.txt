cmake_minimum_required(VERSION 3.18.1)

project("anx")


add_library( # Sets the name of the library.
        anx
        SHARED
        native-lib.cpp)

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

include_directories(
        ${ZMQ_DIR}/include
)

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

target_link_libraries( # Specifies the target library.
        anx
        ${log-lib}
        cppzmq)
