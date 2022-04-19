//
// Created by jiezhuchen on 22-4-19.
//
#include "stdio.h"
#include "stdlib.h"

#include <string.h>
#include <jni.h>

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include "android/log.h"
#include "android/bitmap.h"
#include "matrix.h"
#include "GLWhiteBoardJNIBridge.h"

static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

extern "C" {
    float mRatio;
    int mWidth;
    int mHeight;
    float mProjMatrix[16];
    float mCameraMatrix[16];

    /**openGL初始化**/
    void setupGraphics(int w, int h) {
        glViewport(0, 0, w, h);//设置视口
        float ratio = (float) h / w;//计算宽长比glViewport
        mWidth = w;
        mHeight = h;
        mRatio = ratio;
        frustumM(mProjMatrix, 0, -1, 1, -ratio, ratio, 1, 550);//设置投影矩阵
        setLookAtM(mCameraMatrix, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);//设置摄像机矩阵
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
        glClearColor(0, 0, 0, 0);//设置背景颜色
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL); //还可以
        //开启透明度混合能力
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);  //todo 这个混合导致光影效果有点问题，需要处理一下
        glDisable(GL_DITHER);
        return;
    }
}