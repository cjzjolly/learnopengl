//
// Created by jiezhuchen on 2021/9/6.
//降噪渲染器
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <string.h>
#include <jni.h>
#include "RenderProgramNoiseReduction.h"
#include "android/log.h"


using namespace OPENGL_VIDEO_RENDERER;
static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

RenderProgramNoiseReduction::RenderProgramNoiseReduction() {
    vertShader = GL_SHADER_STRING(
            $#version 300 es\n
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
            $#version 300 es\n
            precision highp float;
            uniform sampler2D sTexture;//纹理输入
            uniform int funChoice;
            uniform float frame;//第几帧
            uniform vec2 resolution;//分辨率
            in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
            out vec4 fragColor;//输出到的片元颜色

            //统计中值滤波:
            void statisticalMedianFilter(vec2 st) {
                float xUnit = 1.0 / resolution[0];
                float yUnit = 1.0 / resolution[1];
                int i;
                int j;
                //放到3×3的矩阵中方便处理
                vec3 textureChannelArr[9];
                for (i = 0; i < 3; i ++) {
                    for (j = 0; j < 3; j ++) {
                        vec2 stPos = vec2(st.s + float(i - 1)  * xUnit, st.t + float(j - 1) * yUnit);
                        textureChannelArr[i * 3 + j] = texture(sTexture, stPos).rgb;
                    }
                }
                //找到中值(todo 还没优化)：
                for (i = 1; i < 9; i ++) {
                    bool flag = true;
                    for (j = 0; j < 9 - i; j++) {
                        float totalI = textureChannelArr[i].r + textureChannelArr[i].g + textureChannelArr[i].b;
                        float totalJ = textureChannelArr[j].r + textureChannelArr[j].g + textureChannelArr[j].b;
                        if (totalI > totalJ) {
                            vec3 temp = textureChannelArr[i];
                            textureChannelArr[i] = textureChannelArr[j];
                            textureChannelArr[j] = temp;
                            flag = false;
                        }
                    }
                    if (flag) {
                        break;
                    }
                }
//                //赋值颜色
                fragColor = vec4((textureChannelArr[0] + textureChannelArr[1]) / 2.0, 1.0);
            }

            void blur(vec2 st) {
                float xUnit = 1.0 / resolution[0];
                float yUnit = 1.0 / resolution[1];
                int i;
                int j;
                //放到3×3的矩阵中方便处理
                vec3 textureChannelArr[9];
                float kernel[9];
                kernel[0] = 1.0; kernel[1] = 2.0; kernel[2] = 1.0;
                kernel[3] = 2.0; kernel[4] = 4.0; kernel[5] = 2.0;
                kernel[6] = 1.0; kernel[7] = 2.0; kernel[8] = 1.0;
                for (i = 0; i < 3; i ++) {
                    for (j = 0; j < 3; j ++) {
                        vec2 stPos = vec2(st.s + float(i - 1)  * xUnit, st.t + float(j - 1) * yUnit);
                        textureChannelArr[i * 3 + j] = texture(sTexture, stPos).rgb;
                    }
                }
                vec3 color;
                for (i = 0; i < 9; i ++) {
                    color = color + textureChannelArr[i] * kernel[i];
                }
                color = color / 16.0;
                fragColor = vec4(color, 1.0);
            }

            void blur2(vec2 st) {
                float xUnit = 1.0 / resolution[0];
                float yUnit = 1.0 / resolution[1];
                int i;
                int j;
                //放到3×3的矩阵中方便处理
                vec3 textureChannelArr[9];
                for (i = 0; i < 3; i ++) {
                    for (j = 0; j < 3; j ++) {
                        vec2 stPos = vec2(st.s + float(i - 1)  * xUnit, st.t + float(j - 1) * yUnit);
                        textureChannelArr[i * 3 + j] = texture(sTexture, stPos).rgb;
                    }
                }
                vec3 color;
                for (i = 0; i < 9; i ++) {
                    color = color + textureChannelArr[i];
                }
                color = color / 9.0;
                fragColor = vec4(color, 1.0);
//            fragColor = vec4(fragVTexCoord, 1.0, 1.0);
            }

            void blur3(vec2 st) {
                float xUnit = 1.0 / resolution[0];
                float yUnit = 1.0 / resolution[1];
                int i;
                int j;
                float minRGB[3];
                float maxRGB[3];
                int minRGBIndex[3];
                int maxRGBIndex[3];
                minRGB[0] = 1.0;
                minRGB[1] = 1.0;
                minRGB[2] = 1.0;
                //放到3×3的矩阵中方便处理
                vec3 textureChannelArr[9];
                for (i = 0; i < 3; i ++) {
                    for (j = 0; j < 3; j ++) {
                        vec2 stPos = vec2(st.s + float(i - 1)  * xUnit, st.t + float(j - 1) * yUnit);
                        textureChannelArr[i * 3 + j] = texture(sTexture, stPos).rgb;
                    }
                }
                for (i = 0; i < 9; i ++) {
                    if (textureChannelArr[i].r > maxRGB[0]) {
                        maxRGB[0] = textureChannelArr[i].r;
                        maxRGBIndex[0] = i;
                    }
                    if (textureChannelArr[i].g > maxRGB[1]) {
                        maxRGB[1] = textureChannelArr[i].g;
                        maxRGBIndex[1] = i;
                    }
                    if (textureChannelArr[i].b > maxRGB[2]) {
                        maxRGB[2] = textureChannelArr[i].b;
                        maxRGBIndex[2] = i;
                    }
                }
                vec3 color;
                for (i = 0; i < 9; i ++) {
                    if (i != maxRGBIndex[0]) {
                        color.r = color.r + textureChannelArr[i].r;
                    }
                    if (i != maxRGBIndex[1]) {
                        color.g = color.g + textureChannelArr[i].g;
                    }
                    if (i != maxRGBIndex[2]) {
                        color.b = color.b + textureChannelArr[i].b;
                    }
                }
                color = color / 8.0;
                fragColor = vec4(color, 1.0);
                //fragColor = vec4(fragVTexCoord, 1.0, 1.0);
            }

            void main() {
//                vec4 color = texture(sTexture, fragVTexCoord);
//                fragColor = vec4(fragVTexCoord, 1.0, 1.0); //cjztest
                statisticalMedianFilter(fragVTexCoord);
//                blur3(fragVTexCoord);
            }
    );
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

RenderProgramNoiseReduction::~RenderProgramNoiseReduction() {
    //cjzmark todo 记得释放资源
}

void RenderProgramNoiseReduction::createRender(float x, float y, float z, float w, float h, int windowW, int windowH) {
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
    mImageProgram = createProgram(vertShader + 1, fragShader + 1);
    //获取程序中顶点位置属性引用"指针"
    mObjectPositionPointer = glGetAttribLocation(mImageProgram.programHandle, "objectPosition");
    //纹理采样坐标
    mVTexCoordPointer = glGetAttribLocation(mImageProgram.programHandle, "vTexCoord");
    //获取程序中顶点颜色属性引用"指针"
    mObjectVertColorArrayPointer = glGetAttribLocation(mImageProgram.programHandle, "objectColor");
    //获取程序中总变换矩阵引用"指针"
    muMVPMatrixPointer = glGetUniformLocation(mImageProgram.programHandle, "uMVPMatrix");
    //渲染方式选择，0为线条，1为纹理
    mGLFunChoicePointer = glGetUniformLocation(mImageProgram.programHandle, "funChoice");
    //渲染帧计数指针
    mFrameCountPointer = glGetUniformLocation(mImageProgram.programHandle, "frame");
    //设置分辨率指针，告诉gl脚本现在的分辨率
    mResoulutionPointer = glGetUniformLocation(mImageProgram.programHandle, "resolution");
}

void RenderProgramNoiseReduction::loadTexture(Textures textures[]) {
    mInputTexturesArray = textures[0].texturePointers;
    mInputTextureWidth = textures[0].width;
    mInputTextureHeight = textures[0].height;
}

void RenderProgramNoiseReduction::loadData(char *data, int width, int height, int pixelFormat, int offset) { //yuvl类数据忽略pixelFormat，根据mYuvKind进行判断

}

//todo
void RenderProgramNoiseReduction::setAlpha(float alpha) {

}

//todo
void RenderProgramNoiseReduction::setBrightness(float brightness) {

}

//todo
void RenderProgramNoiseReduction::setContrast(float contrast) {

}

//todo
void RenderProgramNoiseReduction::setWhiteBalance(float redWeight, float greenWeight, float blueWeight) {

}

void RenderProgramNoiseReduction::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {
    if (mIsDestroyed) {
        return;
    }
    glUseProgram(mImageProgram.programHandle);
    //设置视窗大小及位置
    glBindFramebuffer(GL_FRAMEBUFFER, outputFBOPointer);
    glViewport(0, 0, mWindowW, mWindowH);
    glUniform1i(mGLFunChoicePointer, 1);
    //传入位置信息
    locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
    //开始渲染：
    if (mVertxData != nullptr && mColorBuf != nullptr) {
        //将顶点位置数据送入渲染管线
        glVertexAttribPointer(mObjectPositionPointer, 3, GL_FLOAT, false, 0, mVertxData); //三维向量，size为2
        //将顶点颜色数据送入渲染管线
        glVertexAttribPointer(mObjectVertColorArrayPointer, 4, GL_FLOAT, false, 0, mColorBuf);
        //将顶点纹理坐标数据传送进渲染管线
        glVertexAttribPointer(mVTexCoordPointer, 2, GL_FLOAT, false, 0, mTexCoor);  //二维向量，size为2
        glEnableVertexAttribArray(mObjectPositionPointer); //启用顶点属性
        glEnableVertexAttribArray(mObjectVertColorArrayPointer);  //启用颜色属性
        glEnableVertexAttribArray(mVTexCoordPointer);  //启用纹理采样定位坐标
        float resolution[2];

        switch (drawType) {
            case OPENGL_VIDEO_RENDERER::RenderProgram::DRAW_DATA:
                break;
            case OPENGL_VIDEO_RENDERER::RenderProgram::DRAW_TEXTURE:
                glActiveTexture(GL_TEXTURE0); //激活0号纹理
                glBindTexture(GL_TEXTURE_2D, mInputTexturesArray);
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTexture"), 0); //映射到渲染脚本，获取纹理属性的指针
                resolution[0] = (float) mInputTextureWidth;
                resolution[1] = (float) mInputTextureHeight;
                glUniform2fv(mResoulutionPointer, 1, resolution);
                break;
        }
        glDrawArrays(GL_TRIANGLE_STRIP, 0, /*mPointBufferPos / 3*/ 4); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        glDisableVertexAttribArray(mObjectPositionPointer);
        glDisableVertexAttribArray(mObjectVertColorArrayPointer);
        glDisableVertexAttribArray(mVTexCoordPointer);
    }
}

void RenderProgramNoiseReduction::destroy() {

}