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
#include "RenderProgramYUV.h"

#include <string.h>
#include <jni.h>

#include "android/log.h"

static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

using namespace OPENGL_VIDEO_RENDERER;

char vertShader[] = GL_SHADER_STRING(
        version 300 es
        uniform mat4 uMVPMatrix; //旋转平移缩放 总变换矩阵。物体矩阵乘以它即可产生变换
        in vec3 objectPosition; //物体位置向量，参与运算但不输出给片源

        in vec4 objectColor; //物理颜色向量
        in vec2 vTexCoord; //纹理内坐标
        out vec4 fragObjectColor;//输出处理后的颜色值给片元程序
        out vec2 fragVTexCoord;//输出处理后的纹理内坐标给片元程序


        void main() {
            gl_Position = uMVPMatrix * vec4(objectPosition, 1.0); //设置物体位置
            fragVTexCoord = vTexCoord; //默认无任何处理，直接输出物理内采样坐标
            fragObjectColor = objectColor; //默认无任何处理，输出颜色值到片源
        }
);

char fragShader[] = GL_SHADER_STRING(
        version 300 es
        precision highp float;
        uniform sampler2D textureY;//YPanel纹理输入，glTexImage2d时要设置直字节格式为：GL_LUMINANCE/GL_ALPHA(单个字节), GL_UNSIGNED_BYTE
        uniform sampler2D textureUV;//UV输入，glTexImage2d时要设置直字节格式为：GL_LUMINANCE_ALPHA(双字节), GL_UNSIGNED_BYTE
        uniform sampler2D textureFBO;
        uniform int funChoice;
        uniform float frame;//第几帧
        uniform vec2 resolution;//分辨率
        in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
        in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
        out vec4 fragColor;//输出到的片元颜色

//https://blog.csdn.net/byhook/article/details/84037338 有各种YUV的排列方式
/**yuvtorgb
        R = Y + 1.4075 *（V-128）
       G = Y – 0.3455 *（U –128） – 0.7169 *（V –128）
       B = Y + 1.779 *（U – 128）
**/

        vec3 yuvToRGB(float y, float u, float v) {
            float r = y + 1.370705 * (v - 0.5);
            float g = y - 0.337633 * (u - 0.5) - 0.698001 * (v - 0.5);
            float b = y + 1.732446 * (u - 0.5);
            //    r = clamp(r, 0.0, 1.0);
            //    g = clamp(g, 0.0, 1.0);
            //    b = clamp(b, 0.0, 1.0);
            return vec3(r, g, b);
        }

        void convertYUV420SP(bool reverse, in vec2 fragVTexCoord, out vec4 fragColor) {
            float y = texture(textureY, fragVTexCoord)[0];
            //uvuvuvuv
            float u = texture(textureUV, fragVTexCoord)[3];
            float v = texture(textureUV, fragVTexCoord)[0];
            vec3 rgb;
            if (reverse) {
                rgb = yuvToRGB(y, v, u);//NV21
            } else {
                rgb = yuvToRGB(y, u, v);//NV12
            }
            fragColor = vec4(rgb, 1.0);
        }

        void convertYUV420P(bool reverse, in vec2 fragVTexCoord, out vec4 fragColor) {
            float y = texture(textureY, fragVTexCoord)[0];
            /*
            uuu...
            vvv....
            */
            //????
            float u = texture(textureUV, vec2(fragVTexCoord[0] / 2.0, fragVTexCoord[1] / 2.0))[0];
            float v = texture(textureUV,
                              vec2(fragVTexCoord[0] / 2.0, fragVTexCoord[1] / 2.0 + 0.5))[0];
            vec3 rgb;
            if (reverse) {
                rgb = yuvToRGB(y, v, u);//YV12
            } else {
                rgb = yuvToRGB(y, u, v);//YU12 / I420
            }
            fragColor = vec4(rgb, 1.0);
        }

);


void RenderProgramYUV::createRender() {

}

void RenderProgramYUV::requestRender(int outputFBOTexturePointer, int *texturesPointer,
                                     int dataArrayLength) {

}

extern "C" {
//    JNIEXPORT void JNICALL Java_com_opengldecoder_jnibridge_JniBridge_drawBuffer(JNIEnv *env, jobject activity) {
//
//    }
}