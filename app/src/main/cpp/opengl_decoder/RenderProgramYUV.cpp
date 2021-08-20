//
// Created by jiezhuchen on 2021/6/21.
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <string.h>
#include <jni.h>
#include "RenderProgramYUV.h"
#include "android/log.h"


using namespace OPENGL_VIDEO_RENDERER;
static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

RenderProgramYUV::RenderProgramYUV() {
    //todo #号如何放进去，暂时opengl version声明只能用很奇怪的写法，通过双#号放进去之后，把第一个#号舍弃
    vertShader = GL_SHADER_STRING(
            ##version 300 es\n
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
    fragShader = GL_SHADER_STRING(
            ##version 300 es\n
            precision highp float;
    );

    float tempTexCoord[] =   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
            {
                    1.0, 0.0,
                    0.0, 0.0,
                    1.0, 1.0,
                    0.0, 1.0
            };
    memcpy(mTexCoor, tempTexCoord, sizeof(tempTexCoord));
    float tempColorBuf[] = {
            1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0
    };
    memcpy(mColorBuf, tempColorBuf, sizeof(tempColorBuf));
}

RenderProgramYUV::~RenderProgramYUV() {

}

void RenderProgramYUV::createRender(float x, float y, float z, float w, float h, int windowW, int windowH) {

}

void RenderProgramYUV::loadData(char *data, int width, int height, int pixelFormat, int offset) {

}

void RenderProgramYUV::setAlpha(float alpha) {

}

void RenderProgramYUV::loadTexture(Textures textures[]) {

}

void RenderProgramYUV::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {

}

void RenderProgramYUV::destroy() {

}