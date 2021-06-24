//
// Created by jiezhuchen on 2021/5/19.
//

#include "stdio.h"
#include "stdlib.h"

#include <string.h>
#include <jni.h>

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include "android/log.h"
#include "android/bitmap.h"
#include "android/native_window_jni.h"
#include <sys/time.h>
#include "matrixState.c"


static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)


char *jstringToChar(JNIEnv *env, jstring jstr);

// 由于jvm和c++对中文的编码不一样，因此需要转码。 utf8/16转换成gb2312
char *jstringToChar(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass clsstring = (*env)->FindClass(env, "java/lang/String");
    jstring strencode = (*env)->NewStringUTF(env, "GB2312");
    jmethodID mid = (*env)->GetMethodID(env, clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) (*env)->CallObjectMethod(env, jstr, mid, strencode);
    jsize alen = (*env)->GetArrayLength(env, barr);
    jbyte *ba = (*env)->GetByteArrayElements(env, barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    (*env)->ReleaseByteArrayElements(env, barr, ba, 0);
    return rtn;
}

///*传入surface进行直接绘制的例子，传入颜色涂满整个surface */
JNIEXPORT void JNICALL
Java_com_opengldecoder_jnibridge_JniBridge_drawToSurface(JNIEnv *env, jobject activity,
                                                         jobject surface, jint color) {
    ANativeWindow_Buffer nwBuffer;

    LOGI("ANativeWindow_fromSurface ");
    ANativeWindow *mANativeWindow = ANativeWindow_fromSurface(env, surface);

    if (mANativeWindow == NULL) {
        LOGE("ANativeWindow_fromSurface error");
        return;
    }

    LOGI("ANativeWindow_lock ");
    if (0 != ANativeWindow_lock(mANativeWindow, &nwBuffer, 0)) {
        LOGE("ANativeWindow_lock error");
        return;
    }

    LOGI("ANativeWindow_lock nwBuffer->format ");
    if (nwBuffer.format == WINDOW_FORMAT_RGB_565) {
        int y, x;
        LOGI("nwBuffer->format == WINDOW_FORMAT_RGB_565");
        memset(nwBuffer.bits, color << 8, sizeof(__uint16_t) * nwBuffer.height * nwBuffer.width);
    } else if (nwBuffer.format == WINDOW_FORMAT_RGBA_8888) {
        LOGI("nwBuffer->format == WINDOW_FORMAT_RGBA_8888 ");
        memset(nwBuffer.bits, color, sizeof(__uint32_t) * nwBuffer.height * nwBuffer.width);
    }
    LOGI("ANativeWindow_unlockAndPost ");
    if (0 != ANativeWindow_unlockAndPost(mANativeWindow)) {
        LOGE("ANativeWindow_unlockAndPost error");
        return;
    }

    ANativeWindow_release(mANativeWindow);
    LOGI("ANativeWindow_release ");
}

void setupGraphics(int w, int h, float *bgColor)//初始化函数
{
    glViewport(0, 0, w, h);//设置视口
    float ratio = (float) h / w;//计算宽长比glViewport
    setProjectFrustum(-1, 1, -ratio, ratio , 1, 50);//设置投影矩阵
    setCamera(0, 0, 1, 0, 0, 0, 0, 1, 0);//设置摄像机矩阵
    setInitStack();//初始化变换矩阵
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
    glClearColor(bgColor[0], bgColor[1], bgColor[2], bgColor[3]);//设置背景颜色
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL); //还可以
    //开启透明度混合能力
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);  //todo 这个混合导致光影效果有点问题，需要处理一下
    glDisable(GL_DITHER);
    return;
}

/**安卓系统在有GLSurfaceview的情况下不需要进行EGL相关操作**/
void androidNativeInitGL(int viewPortW, int viewPortH) {
    float bgColor[] = {1.0f, 1.0f, 1.0f, 1.0f};
    setupGraphics(viewPortW, viewPortH, bgColor);
}

JNIEXPORT void JNICALL
Java_com_opengldecoder_jnibridge_JniBridge_nativeGLInit(JNIEnv *env, jobject activity,
                                                        jint viewPortWidth, jint viewPortHeight) {
    androidNativeInitGL(viewPortWidth, viewPortHeight);
}

JNIEXPORT void JNICALL
Java_com_opengldecoder_jnibridge_JniBridge_draw(JNIEnv *env, jobject activity) {
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
    glClearColor(1, 0, 0, 1);
}