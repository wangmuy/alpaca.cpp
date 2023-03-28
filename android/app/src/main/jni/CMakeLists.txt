cmake_minimum_required(VERSION 3.8)
set(TARGET alpacajni)
project(${TARGET})

add_library(NativeUtils # library name
    SHARED # SHARED/STATIC
    org_alpaca_test_NativeUtils.cpp # relative path
)

include_directories(
    .
)

set(ALPACA_BUILD_TYPE "lib" CACHE STRING "app or lib")
add_subdirectory(../../../../.. alpaca) # root/android/app/src/main/jni

find_library(
    log-lib
    log
)

target_link_libraries(
    NativeUtils
    ${log-lib}
    ggml
    chatbase
)
