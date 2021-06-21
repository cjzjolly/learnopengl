# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)
#project(zbar LANGUAGES C VERSION 2.0.2)
project(opengl_decoder)

#SET(zbar_sdk_dir ${CMAKE_SOURCE_DIR}/src/main/cpp/opengl_decoder)
message(AUTHOR_WARNING ${CMAKE_CURRENT_SOURCE_DIR})
message(AUTHOR_WARNING ${opengl_decoder})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

#INCLUDE_DIRECTORIES("/lib")

add_library( # Sets the name of the library.
             opengl_decoder

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             opengl_native_lib.c
        decode_buffer.cpp
        decode_buffer.h
             )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(log-lib log)
find_library(android-lib android)
find_library(EGL-lib EGL)
find_library(GLESv2-lib GLESv2)
find_library(OpenSLES-lib OpenSLES)
find_library(dl-lib dl)
find_library(z-lib z)

target_link_libraries(
        opengl_decoder
        ${log-lib}
                ${android-lib}
                ${EGL-lib}
                ${GLESv2-lib}
                ${OpenSLES-lib}
                ${dl-lib}
                ${z-lib}
)


#add_library(zbar_sdk SHARED IMPORTED )
#set_target_properties(zbar_sdk PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/lib/${ANDROID_ABI}/libiconv.so)
message(AUTHOR_WARNING ${PROJECT_SOURCE_DIR})


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#target_link_libraries( # Specifies the target library.
#                       opengl_decoder
#
#                       # Links the target library to the log library
#                       # included in the NDK.
#                       ${log-lib} ${android-lib}
#                       zbar_sdk
#                       )

