# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)
#project(zbar LANGUAGES C VERSION 2.0.2)
project(ffmpeg)

#SET(zbar_sdk_dir ${CMAKE_SOURCE_DIR}/src/main/cpp/ffmpeg)
message(AUTHOR_WARNING ${CMAKE_CURRENT_SOURCE_DIR})
message(AUTHOR_WARNING ${ffmpeg})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

#INCLUDE_DIRECTORIES("/lib")

add_library(ffmpeg-lib
        SHARED
        IMPORTED)

set_target_properties(ffmpeg-lib
        PROPERTIES
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libavcodec-57.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libavdevice-57.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libavfilter-6.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libavformat-57.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libavutil-55.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libpostproc-54.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libswresample-2.so
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/libswscale-4.so
        )

add_library( # Sets the name of the library.
        ffmpeg

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        # Associated headers in the same location as their source
        # file are automatically included.
        ffmpeg.c
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
        ffmpeg
        ${log-lib}
        ${android-lib}
        ${EGL-lib}
        ${GLESv2-lib}
        ${OpenSLES-lib}
        ${dl-lib}
        ${z-lib}
        #数学库:
        m
)


#add_library(zbar_sdk SHARED IMPORTED )
#set_target_properties(zbar_sdk PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/lib/${ANDROID_ABI}/libiconv.so)
message(AUTHOR_WARNING ${PROJECT_SOURCE_DIR})


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#target_link_libraries( # Specifies the target library.
#                       ffmpeg
#
#                       # Links the target library to the log library
#                       # included in the NDK.
#                       ${log-lib} ${android-lib}
#                       zbar_sdk
#                       )

