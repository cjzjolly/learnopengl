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
#include "OpenGLNativeRender.h"
#include "RenderProgramImage.h"
#include "RenderProgramConvolution.h"


static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)


OpenGLNativeRender mOpenGLNativeLib;

// 由于jvm和c++对中文的编码不一样，因此需要转码。 utf8/16转换成gb2312
char *jstringToChar(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}

void OpenGLNativeRender::setupGraphics(int w, int h, float *bgColor)//初始化函数
{
    glViewport(0, 0, w, h);//设置视口
    float ratio = (float) h / w;//计算宽长比glViewport
    mWidth = w;
    mHeight = h;
    mRatio = ratio;
    frustumM(mProjMatrix, 0, -1, 1, -ratio, ratio, 1, 50);//设置投影矩阵
    setLookAtM(mCameraMatrix, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);//设置摄像机矩阵
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
    glClearColor(bgColor[0], bgColor[1], bgColor[2], bgColor[3]);//设置背景颜色
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL); //还可以
    //开启透明度混合能力
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);  //todo 这个混合导致光影效果有点问题，需要处理一下
    glDisable(GL_DITHER);
    //创建图层：
    mLayer = new Layer(-1, -ratio, 0, 2, ratio * 2, w, h);
    //添加渲染器:
    RenderProgramImage *mRenderProgramImage = new RenderProgramImage();
    int ww = 482;
    int hh = 678;
//    mRenderProgramImage->createRender(-1, -ratio, 0, 2 * ww / hh * ratio, ratio * 2, w, h);
    mRenderProgramImage->createRender(-1, -ratio, 0, 2, ratio * 2, w, h);
    float kernel[] = {
            1.0, 1.0, 1.0,
            1.0, -7.0, 1.0,
            1.0, 1.0, 1.0
    };
    RenderProgramConvolution *renderProgramCornerPick = new RenderProgramConvolution(kernel);
    renderProgramCornerPick->createRender(-1, -ratio, 0, 2, ratio * 2, w, h);
    renderProgramCornerPick->setAlpha(0.8);
    mLayer->addRenderProgram(mRenderProgramImage);
//    mLayer->addRenderProgram(renderProgramCornerPick);
    return;
}

void OpenGLNativeRender::drawRGBA(char *buf, int w, int h) {
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
    mLayer->loadData(buf, w, h, GL_RGBA, 0);  //todo 修改接口，set数据的接口和draw分离，不要每次都刷新
    //绘制到目标framebuffer，默认使用屏幕0
//    renderProgramCornerPick->rotate(1, 0, 0, 1);
    mLayer->drawTo(mCameraMatrix, mProjMatrix, 0, mWidth, mHeight, Layer::DRAW_DATA);
}

/**安卓系统在有GLSurfaceview的情况下不需要进行EGL相关操作**/
void androidNativeInitGL(int viewPortW, int viewPortH) {
    float bgColor[] = {0.0f, 0.0f, 0.0f, 0.0f};
    mOpenGLNativeLib.setupGraphics(viewPortW, viewPortH, bgColor);
}

extern "C" {
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

    JNIEXPORT void JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_nativeGLInit(JNIEnv *env, jobject activity,
                                                            jint viewPortWidth, jint viewPortHeight) {
        androidNativeInitGL(viewPortWidth, viewPortHeight);
    }

    JNIEXPORT void JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_drawRGBABitmap(JNIEnv *env, jobject activity, jobject bmp, jint bmpW, jint bmpH) {
        uint32_t* sourceData;
        int result = AndroidBitmap_lockPixels(env, bmp, (void**)& sourceData); //指针变量本身有内存地址，所以可以取指针的指针来让函数引用放数据
        if (result < 0) {
            return;
        }
        mOpenGLNativeLib.drawRGBA((char *) sourceData, bmpW, bmpH);
        AndroidBitmap_unlockPixels(env, bmp);
    }
}