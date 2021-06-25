//
// Created by jiezhuchen on 2021/5/19.
//

#include "stdio.h"
#include "stdlib.h"

#include <string.h>
#include <jni.h>
#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libswscale/swscale.h"
#include "android/log.h"


static const char *TAG = "ffmpeg";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

//全局变量区：
AVFormatContext *mFormatCtx = NULL;

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

//https://blog.csdn.net/leixiaohua1020/article/details/8652605
//https://blog.csdn.net/leixiaohua1020/article/details/42181571
/**初始化FFMPEG**/
void initFFMpeg() {
    //FFmpeg
    struct SwsContext *img_convert_ctx;
    av_register_all();
//    avformat_network_init();

    mFormatCtx = avformat_alloc_context();
}

/**播放文件**/
int openFile(char *path) {
    int i, videoindex = -1;
    AVCodecContext *pCodecCtx;
    AVCodec *pCodec;
    AVFrame *pFrame, *pFrameYUV;
    AVPacket packet;
    AVCodecParserContext *pCodecParserCtx = NULL;
    int got_picture;
    if (mFormatCtx == NULL) {
        return -1;
    }
    if (avformat_open_input(&mFormatCtx, path, NULL, NULL) != 0) {
        LOGI("Couldn't open input stream.\n");
        return -1;
    }
//    if (avformat_find_stream_info(path, NULL) < 0) {
//        LOGI("Couldn't find stream information.\n");
//        return -1;
//    }
    //寻找第一个视频帧:
    for (i = 0; i < mFormatCtx->nb_streams; i++)
        if (mFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoindex = i;
            break;
        }
    if (videoindex == -1) {
        LOGI("Didn't find a video stream.\n");
        return -1;
    }
    //寻找视频流对应的解码器：
    pCodecCtx = mFormatCtx->streams[videoindex]->codec;
    //avcodec_register_all();
    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGI("Codec not found.\n");
        return -1;
    }
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGI("Could not open codec.\n");
        return -1;
    }
    //获取第一帧的相关信息
    pCodecParserCtx = av_parser_init(pCodecCtx->codec_id);
    //解码第一帧:
    switch (pCodecParserCtx->pict_type) {
        case AV_PICTURE_TYPE_I:
            LOGI("Type:I\t");
            break;
        case AV_PICTURE_TYPE_P:
            LOGI("Type:P\t");
            break;
        case AV_PICTURE_TYPE_B:
            LOGI("Type:B\t");
            break;
        default:
            LOGI("Type:Other\t");
            break;
    }
    pFrame = av_frame_alloc();
    av_init_packet(&packet);
    LOGI("duration:%d", packet.duration);
//    int ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, &packet);
    int ret = avcodec_receive_frame(pCodecCtx, pFrame);
    if (ret < 0) {
        LOGI("Decode Error:%d.\n", ret);
        return ret;
    }
    //如果顺利解出了帧
    if (got_picture) {
        //Y, U, V
        for (int i = 0; i < pFrame->height; i++) {

        }
        for (int i = 0; i < pFrame->height / 2; i++) {

        }
        for (int i = 0; i < pFrame->height / 2; i++) {

        }

        LOGI("Succeed to decode 1 frame!\n");
    }
}


JNIEXPORT void JNICALL Java_com_ffmpeg_FFMpegUtil_version(JNIEnv *env, jobject activity) {
    unsigned int ver = avcodec_version();
    initFFMpeg();
//    int ret = fopen("/sdcard/Download/bAeqdUAbDOQA.mp4", "r");
//    LOGI("shit:%d", ret);
//    fclose(ret);
//    openFile("/sdcard/Download/bAeqdUAbDOQA.mp4");
    openFile("/sdcard/DCIM/Camera/VID_20170202_084126.mp4");
    LOGI("cjztest: version:%d", ver);
}