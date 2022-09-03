# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)
add_compile_options(
        -fno-omit-frame-pointer
        -fexceptions
        -Wall
)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99 -mfloat-abi=soft -DANDROID")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -mfloat-abi=soft -DANDROID")
# 生成中间文件，便于debug
set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -save-temps=obj")
set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -save-temps=obj")
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.



ADD_SUBDIRECTORY(src/main/cpp/opengl_decoder)
ADD_SUBDIRECTORY(src/main/cpp/ffmpeg)

#暂时注释白板的代码不然编译不过
#ADD_SUBDIRECTORY(src/main/cpp/gl_whiteboard)

#add_library(log-lib)

