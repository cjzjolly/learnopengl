//
// Created by jiezhuchen on 2021/6/21.
//

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/native_window.h>
//#include <ui/GraphicBuffer.h>
#include <dlfcn.h>
#include "decode_buffer.h"

#include <string.h>
#include <jni.h>

#include "android/log.h"

static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)


DecodeBuffer mDecodeBuffer;

void DecodeBuffer::drawBuffer(char *data) {
    if (mRenderProgramImage == nullptr) {
        mRenderProgramImage = new RenderProgramImage();
        mRenderProgramImage->createRender()
    }
}


extern "C" {
    JNIEXPORT void JNICALL Java_com_opengldecoder_jnibridge_JniBridge_drawBuffer(JNIEnv *env, jobject activity) {
        mDecodeBuffer.drawBuffer(nullptr);
    }
}