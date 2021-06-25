# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)
project(ffmpeg_caller)

#SET(zbar_sdk_dir ${CMAKE_SOURCE_DIR}/src/main/cpp/ffmpeg)
#SET(ffmpeg_dir ${CMAKE_SOURCE_DIR}/src/main/cpp/ffmpeg)
message(AUTHOR_WARNING ${CMAKE_CURRENT_SOURCE_DIR})
message(AUTHOR_WARNING ${ffmpeg_caller})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

#INCLUDE_DIRECTORIES("/lib")

add_library( # Sets the name of the library.
        ffmpeg_caller

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        # Associated headers in the same location as their source
        # file are automatically included.
        ffmpeg.c
        )

include_directories(
        ${PROJECT_SOURCE_DIR}/include
)

#添加lib，SHARED类型，是IMPORTED 引入的库
add_library(avcodec
        SHARED
        IMPORTED)
#设置 库的属性   里面是名称 ，属性：引入地址把我们的真实地址填写进去
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libavcodec-57.so
        )

add_library(libavfilter
        SHARED
        IMPORTED)
set_target_properties(libavfilter
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libavfilter-6.so)

add_library(libavformat
        SHARED
        IMPORTED)
set_target_properties(libavformat
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libavformat-57.so)

add_library(libavutil
        SHARED
        IMPORTED)
set_target_properties(libavutil
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libavutil-55.so)

add_library(libswresample
        SHARED
        IMPORTED)
set_target_properties(libswresample
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libswresample-2.so)

add_library(libswscale
        SHARED
        IMPORTED)
set_target_properties(libswscale
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libswscale-4.so)



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
        ffmpeg_caller
        avcodec
        libavfilter
        libavformat
        libavutil
        libswresample
        libswscale
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

