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
#include "RenderProgramOESTexture.h"


static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

/**安卓JNI接入层**/


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
    return;
}


extern "C" {
    /**图层链表结点**/
    struct ListElement {
        Layer *layer;
        struct ListElement* next;
    };
    /**创造一个链表，用于存储Layer对象**/
    ListElement* mLayerList = nullptr;

    /**渲染器类型枚举器 todo java要调用，则也要抄一份**/
    enum RENDER_PROGRAM_KIND {
        RENDER_OES_TEXTURE = 0, //OES纹理渲染
        RENDER_YUV = 1, //YUV数据或纹理渲染
        RENDER_CONVOLUTION = 2, //添加卷积处理
    };

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

    /**安卓系统初始化EGL等，但分情况，安卓系统在有GLSurfaceview的情况下不需要进行EGL相关操作**/
    JNIEXPORT void JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_nativeGLInit(JNIEnv *env, jobject activity,
                                                            jint viewPortWidth, jint viewPortHeight) {
        float bgColor[] = {0.0f, 0.0f, 0.0f, 0.0f};
        mOpenGLNativeLib.setupGraphics(viewPortWidth, viewPortHeight, bgColor);
        LOGI("cjztest Java_com_opengldecoder_jnibridge_JniBridge_nativeGLInit, width:%d, height:%d", viewPortWidth, viewPortHeight);
    }

//    JNIEXPORT void JNICALL
//    Java_com_opengldecoder_jnibridge_JniBridge_drawRGBABitmap(JNIEnv *env, jobject activity, jobject bmp, jint bmpW, jint bmpH) {
//        uint32_t* sourceData;
//        int result = AndroidBitmap_lockPixels(env, bmp, (void**)& sourceData); //指针变量本身有内存地址，所以可以取指针的指针来让函数引用放数据
//        if (result < 0) {
//            return;
//        }
//        mOpenGLNativeLib.drawRGBA((char *) sourceData, bmpW, bmpH);
//        AndroidBitmap_unlockPixels(env, bmp);
//    }

    /**添加一个图层
       @return 返回图层对象的内存地址**/
    JNIEXPORT jlong JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_addLayer(JNIEnv *env, jobject activit, jint texturePointer, jintArray textureWidthAndHeight, jlong dataPointer,
                                                        jintArray dataWidthAndHeight,
                                                        int dataPixelFormat) {
        jint *dataWidthAndHeightPointer = env->GetIntArrayElements(dataWidthAndHeight, JNI_FALSE);
        jint *textureWidthAndHeightPointer = env->GetIntArrayElements(textureWidthAndHeight, JNI_FALSE);
        Layer *layer = new Layer(-1, -mOpenGLNativeLib.mRatio, 0, 2, mOpenGLNativeLib.mRatio * 2, mOpenGLNativeLib.mWidth, mOpenGLNativeLib.mHeight); //创建铺满全屏的图层;
        //载入数据：
        LOGI("cjztest, Java_com_opengldecoder_jnibridge_JniBridge_addLayer containerW:%d, containerH:%d, w:%d, h:%d", mOpenGLNativeLib.mWidth, mOpenGLNativeLib.mHeight, textureWidthAndHeightPointer[0], textureWidthAndHeightPointer[1]);
        layer->loadTexture(texturePointer, textureWidthAndHeightPointer[0], textureWidthAndHeightPointer[1]);
        layer->loadData((char *) dataPointer, dataWidthAndHeightPointer[0], dataWidthAndHeightPointer[1], dataPixelFormat, 0);
        if (mLayerList) {
            struct ListElement* cursor = mLayerList;
            while (cursor->next) {
                cursor = cursor->next;
            }
            cursor->next = (struct ListElement*) malloc(sizeof(struct ListElement));
            cursor->next->layer = layer;
            cursor->next->next = nullptr;
        } else {
            mLayerList = (struct ListElement*) malloc(sizeof(struct ListElement));
            mLayerList->layer = layer;
            mLayerList->next = nullptr;
        }
        env->ReleaseIntArrayElements(dataWidthAndHeight, dataWidthAndHeightPointer, JNI_FALSE);
        env->ReleaseIntArrayElements(textureWidthAndHeight, textureWidthAndHeightPointer, JNI_FALSE);
        return (jlong) layer;
    }


    //todo 删除一个图层
    /**@param layerPointer 要删除的图层的内存地址**/
    JNIEXPORT void JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_removeLayer(JNIEnv *env, jobject activity, jlong layerPointer) {
        return;
    }

    /**为指定图层添加渲染器
    @param layerPointer 图层的内存地址**/
    JNIEXPORT jlong JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_addRenderForLayer(JNIEnv *env, jobject activity,
                                                                 jlong layerPointer,
                                                                 int renderProgramKind) {

        Layer *layer = (Layer *) layerPointer;
        RenderProgram *resultProgram = nullptr;
        switch (renderProgramKind) {
            default:
                break;
            //创建OES纹理渲染器
            case RENDER_OES_TEXTURE: {
                RenderProgramOESTexture *renderProgramOesTexture = new RenderProgramOESTexture();
                renderProgramOesTexture->createRender(-1, -mOpenGLNativeLib.mRatio, 0, 2,
                                                      mOpenGLNativeLib.mRatio * 2,
                                                      mOpenGLNativeLib.mWidth,
                                                      mOpenGLNativeLib.mHeight);
                layer->addRenderProgram(renderProgramOesTexture);
                resultProgram = renderProgramOesTexture;
                break;
            }
            case RENDER_YUV: {
                //todo 暂时未完成
                break;
            }
            //创建卷积渲染器
            case RENDER_CONVOLUTION: {
                float kernel[] = {
                        1.0, 1.0, 1.0,
                        1.0, -7.0, 1.0,
                        1.0, 1.0, 1.0
                };
                RenderProgramConvolution *renderProgramConvolution = new RenderProgramConvolution(
                        kernel);
                renderProgramConvolution->createRender(-1, -mOpenGLNativeLib.mRatio, 0, 2,
                                                       mOpenGLNativeLib.mRatio * 2,
                                                       mOpenGLNativeLib.mWidth,
                                                       mOpenGLNativeLib.mHeight);
                renderProgramConvolution->setAlpha(0.8);  //todo cjzmark 测试透明度用
                layer->addRenderProgram(renderProgramConvolution);
                resultProgram = renderProgramConvolution;
                break;
            }
        }
        return (jlong) resultProgram;
    }

    /**todo 渲染图层数据，安卓端使用了OES接入，所以drawType使用DRAW_TEXTURE,渲染图层到系统目标framebuffer
     * 默认使用0号，也就是系统自身的屏幕FBO**/
    JNIEXPORT void JNICALL
    Java_com_opengldecoder_jnibridge_JniBridge_renderLayer(JNIEnv *env, jobject activity, jint fboPointer, jint fboWidth, jint fboHeight) {
        //防止画面残留：
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
        glClearColor(0.0, 0.0, 0.0, 0.0);
        //遍历图层并渲染
        if (mLayerList) {
            struct ListElement* cursor = mLayerList;
            while (cursor) {
                cursor->layer->drawTo(mOpenGLNativeLib.mCameraMatrix, mOpenGLNativeLib.mProjMatrix, fboPointer, fboWidth, fboHeight, Layer::DRAW_TEXTURE);
                cursor = cursor->next;
            }
        }
//        //cjztest jni 连通性测试，通过 start
//        //        LOGI("cjztest Java_com_opengldecoder_jnibridge_JniBridge_renderLayer");
//        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
//        glClearColor(1, 0, 0, 0.5);
//        //cjztest jni 连通性测试 end
        return;
    }

}