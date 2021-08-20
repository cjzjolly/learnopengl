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

RenderProgramYUV::RenderProgramYUV(RENDER_PROGRAM_YUV_KIND yuvKind) {
    //cjzmark todo 不知道为何，OES纹理无法使用OpenGLES 3
    vertShader = GL_SHADER_STRING(
            uniform mat4 uMVPMatrix; //旋转平移缩放 总变换矩阵。物体矩阵乘以它即可产生变换
            attribute vec3 objectPosition; //物体位置向量，参与运算但不输出给片源

            attribute vec4 objectColor; //物理颜色向量
            attribute vec2 vTexCoord; //纹理内坐标
            varying vec4 fragObjectColor;//输出处理后的颜色值给片元程序
            varying vec2 fragVTexCoord;//输出处理后的纹理内坐标给片元程序

            void main() {
                    gl_Position = uMVPMatrix * vec4(objectPosition, 1.0); //设置物体位置
                    fragVTexCoord = vTexCoord; //默认无任何处理，直接输出物理内采样坐标
                    fragObjectColor = objectColor; //默认无任何处理，输出颜色值到片源
            }
    );

    fragShader = GL_SHADER_STRING(
            ##extension GL_OES_EGL_image_external : require
            precision highp float;
            uniform sampler2D textureY;//YPanel纹理输入，glTexImage2d时要设置直字节格式为：GL_LUMINANCE/GL_ALPHA(单个字节), GL_UNSIGNED_BYTE
            uniform sampler2D textureUV;//UV输入，glTexImage2d时要设置直字节格式为：GL_LUMINANCE_ALPHA(双字节), GL_UNSIGNED_BYTE
            uniform samplerExternalOES oesTexture;//OES形式的纹理输入
            uniform int funChoice;
            uniform float frame;//第几帧
            uniform vec2 resolution;//容器的分辨率
            uniform vec2 videoResolution;//视频自身的分辨率
            varying vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            varying vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序

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
                    float v = texture(textureUV, vec2(fragVTexCoord[0] / 2.0, fragVTexCoord[1] / 2.0 + 0.5))[0];
                    vec3 rgb;
                    if (reverse) {
                            rgb = yuvToRGB(y, v, u);//YV12
                    } else {
                            rgb = yuvToRGB(y, u, v);//YU12 / I420
                    }
                    fragColor = vec4(rgb, 1.0);
            }

            void main() {
                switch (funChoice) {
                        default :
                        case 0:
                            convertYUV420SP(false, fragVTexCoord, fragColor);//uvuv
                            break;
                        case 1:
                            convertYUV420SP(true, fragVTexCoord, fragColor);//vuvu
                            break;
                        case 2:
                            convertYUV420P(false, fragVTexCoord, fragColor);//uuuvvv
                            break;
                        case 3:
                            convertYUV420P(true, fragVTexCoord, fragColor);//vvvuuu
                            break;
                        case 4: //渲染oes纹理
                            gl_FragColor = vec4(texture2D(oesTexture, xy).rgb, fragObjectColor.a);
                            break;
                }
            }
    );
    //记录用户想使用哪一种fragShader的YUV格式渲染函数
    mYuvKind = yuvKind;
    float tempTexCoord[] =   //纹理内采样坐标,类似于canvas坐标
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
    //cjzmark todo 记得释放资源
}

void RenderProgramYUV::createEmptyTexture(GLuint *textureID, int imgWidth, int imgHeight, int pixelFormat) {
    glGenTextures(1, textureID); //只要值不重复即可
    //UV纹理初始化
    glBindTexture(GL_TEXTURE_2D, *textureID);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
    glTexImage2D(GL_TEXTURE_2D, 0, pixelFormat, imgWidth, imgHeight, 0, pixelFormat, GL_UNSIGNED_BYTE, NULL); //因为这里使用了双字节，所以纹理大小对比使用单字节的Y通道纹理，宽度首先要缩小一般，而uv层高度本来就只有y层一般，所以高度也除以2
}

void RenderProgramYUV::createRender(float x, float y, float z, float w, float h, int windowW, int windowH) {
    mWindowW = windowW;
    mWindowH = windowH;
    initObjMatrix(); //使物体矩阵初始化为单位矩阵，否则接下来的矩阵操作因为都是乘以0而无效
    float vertxData[] = {
            x + w, y, z,
            x, y, z,
            x + w, y + h, z,
            x, y + h, z,
    };
    memcpy(mVertxData, vertxData, sizeof(vertxData));
    mYuvProgram = createProgram(vertShader, fragShader + 1);
    //获取程序中顶点位置属性引用"指针"
    mObjectPositionPointer = glGetAttribLocation(mYuvProgram.programHandle, "objectPosition");
    //纹理采样坐标
    mVTexCoordPointer = glGetAttribLocation(mYuvProgram.programHandle, "vTexCoord");
    //获取程序中顶点颜色属性引用"指针"
    mObjectVertColorArrayPointer = glGetAttribLocation(mYuvProgram.programHandle, "objectColor");
    //获取程序中总变换矩阵引用"指针"
    muMVPMatrixPointer = glGetUniformLocation(mYuvProgram.programHandle, "uMVPMatrix");
    //渲染方式选择，0为线条，1为纹理
    mGLFunChoicePointer = glGetUniformLocation(mYuvProgram.programHandle, "funChoice");
    //渲染帧计数指针
    mFrameCountPointer = glGetUniformLocation(mYuvProgram.programHandle, "frame");
    //设置分辨率指针，告诉gl脚本现在的分辨率
    mResoulutionPointer = glGetUniformLocation(mYuvProgram.programHandle, "resolution");
}

void RenderProgramYUV::loadData(char *data, int width, int height, int pixelFormat, int offset) { //yuvl类数据忽略pixelFormat，根据mYuvKind进行判断
    //如果长宽和上次的数据不一致，或者第一次创建yuv纹理，才创建
    if (width != mDataWidth || height != mDataHeight) {
        //如果上次的纹理还在，先清理一下:
        if (mGenYTextureId != 1) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, mGenYTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, 0, 0, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, nullptr); //更新一个空图释放显存。
            glDeleteTextures(1, &mGenYTextureId);
        }
        if (mGenUVTextureId != 1) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, mGenUVTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, 0, 0, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, nullptr); //更新一个空图释放显存。
            glDeleteTextures(1, &mGenUVTextureId);
        }
        //还原pointer id，等待重新分配
        mGenYTextureId = -1;
        mGenUVTextureId = -1;
    }
    mDataWidth = width;
    mDataHeight = height;
    //生成textureY纹理
    if (mGenUVTextureId == -1) {
        createEmptyTexture(&mGenYTextureId, width, height, GL_LUMINANCE);
    }
    //生成textureUV纹理
    if (mGenUVTextureId == -1) {
        switch (mYuvKind) {
            default:
            case YUV_420SP_UVUV:
            case YUV_420SP_VUVU:
                createEmptyTexture(&mGenUVTextureId, width / 2, height / 2, GL_LUMINANCE_ALPHA);
                break;
            case YUV_420P_UUVV:
            case YUV_420P_VVUU:
                createEmptyTexture(&mGenUVTextureId, width, height / 2, GL_LUMINANCE);
                break;
        }
    }
    //todo 加载数据
}

void RenderProgramYUV::setAlpha(float alpha) {

}

void RenderProgramYUV::loadTexture(Textures textures[]) {

}

void RenderProgramYUV::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {

}

void RenderProgramYUV::destroy() {

}