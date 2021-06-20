//
// Created by jiezhuchen on 2021/5/19.
//

#include "stdio.h"
#include "stdlib.h"

#include <string.h>
#include <jni.h>

#include "android/log.h"
#include "android/bitmap.h"
#include "android/native_window_jni.h"
#include <sys/time.h>

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
    jbyteArray barr = (jbyteArray)(*env)->CallObjectMethod(env, jstr, mid, strencode);
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
//JNIEXPORT void JNICALL Java_com_cjz_testSurfaceWithNativeGL_GLUtil_drawToSurface(
//        JNIEnv * env, jobject activity, jobject surface, jint color)
//{
//ANativeWindow_Buffer nwBuffer;
//
//LOGI("ANativeWindow_fromSurface ");
//ANativeWindow * mANativeWindow = ANativeWindow_fromSurface(env, surface);
//
//if (mANativeWindow == NULL) {
//LOGE("ANativeWindow_fromSurface error");
//return;
//}
//
//LOGI("ANativeWindow_lock ");
//if (0 != ANativeWindow_lock(mANativeWindow, &nwBuffer, 0)) {
//LOGE("ANativeWindow_lock error");
//return;
//}
//
//LOGI("ANativeWindow_lock nwBuffer->format ");
//if (nwBuffer.format == WINDOW_FORMAT_RGB_565) {
//int y, x;
//LOGI("nwBuffer->format == WINDOW_FORMAT_RGB_565");
//memset(nwBuffer.bits, color << 8, sizeof(__uint16_t) * nwBuffer.height * nwBuffer.width);
//} else if (nwBuffer.format == WINDOW_FORMAT_RGBA_8888) {
//LOGI("nwBuffer->format == WINDOW_FORMAT_RGBA_8888 ");
//memset(nwBuffer.bits, color, sizeof(__uint32_t) * nwBuffer.height * nwBuffer.width);
//}
//LOGI("ANativeWindow_unlockAndPost ");
//if(0 !=ANativeWindow_unlockAndPost(mANativeWindow)){
//LOGE("ANativeWindow_unlockAndPost error");
//return;
//}
//
//ANativeWindow_release(mANativeWindow);
//LOGI("ANativeWindow_release ");
//}