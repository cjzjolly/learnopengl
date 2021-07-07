//
// Created by jiezhuchen on 2021/7/7.
//
#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <string.h>
#include <jni.h>
#include "RenderProgramCornerPick.h"
#include "android/log.h"


using namespace OPENGL_VIDEO_RENDERER;
static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

RenderProgramCornerPick::RenderProgramCornerPick() {
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
            uniform sampler2D textureFBO;//纹理输入
            uniform int funChoice;
            uniform float frame;//第几帧
            uniform vec2 resolution;//分辨率
            in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
            out vec4 fragColor;//输出到的片元颜色

            void kernalEffect(vec2 TexCoords)
            {
                    float offset = 1.0 / resolution.x;
                    vec2 offsets[9] = vec2[](
                            vec2(-offset, offset), // 左上
                            vec2(0.0, offset), // 正上
                            vec2(offset, offset), // 右上
                            vec2(-offset, 0.0), // 左
                            vec2(0.0, 0.0), // 中
                            vec2(offset, 0.0), // 右
                            vec2(-offset, -offset), // 左下
                            vec2(0.0, -offset), // 正下
                            vec2(offset, -offset)// 右下
                    );
                    float kernel[9] = float[](
                            1.0, 1.0, 1.0,
                            1.0, -7.0, 1.0,
                            1.0, 1.0, 1.0
                    );

                    vec3 sampleTex[9];
                    for (int i = 0; i < 9; i++)
                    {
                            sampleTex[i] = vec3(texture(textureFBO, TexCoords.st + offsets[i]));
                    }
                    vec3 col = vec3(0.0);
                    for (int i = 0; i < 9; i++)
                    col += sampleTex[i] * kernel[i];

                    fragColor = vec4(col, 1.0) * fragObjectColor;
            }

            void main() {
                kernalEffect(vec2(fragVTexCoord.s, fragVTexCoord.t));
            }
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

RenderProgramCornerPick::~RenderProgramCornerPick() {
    destroy();
}

void RenderProgramCornerPick::createRender(float x, float y, float z, float w, float h, int windowW,
                                      int windowH) {
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
    mCornerPickProgram = createProgram(vertShader + 1, fragShader + 1);
    //获取程序中顶点位置属性引用"指针"
    mObjectPositionPointer = glGetAttribLocation(mCornerPickProgram.programHandle, "objectPosition");
    //纹理采样坐标
    mVTexCoordPointer = glGetAttribLocation(mCornerPickProgram.programHandle, "vTexCoord");
    //获取程序中顶点颜色属性引用"指针"
    mObjectVertColorArrayPointer = glGetAttribLocation(mCornerPickProgram.programHandle, "objectColor");
    //获取程序中总变换矩阵引用"指针"
    muMVPMatrixPointer = glGetUniformLocation(mCornerPickProgram.programHandle, "uMVPMatrix");
    //渲染方式选择，0为线条，1为纹理
    mGLFunChoicePointer = glGetUniformLocation(mCornerPickProgram.programHandle, "funChoice");
    //渲染帧计数指针
    mFrameCountPointer = glGetUniformLocation(mCornerPickProgram.programHandle, "frame");
    //设置分辨率指针，告诉gl脚本现在的分辨率
    mResoulutionPointer = glGetUniformLocation(mCornerPickProgram.programHandle, "resolution");
}

void RenderProgramCornerPick::loadData(char *data, int width, int height, int pixelFormat, int offset) {
    if (!mIsTexutresInited) {
        glUseProgram(mCornerPickProgram.programHandle);
        glGenTextures(1, texturePointers);
        mGenTextureId = texturePointers[0];
        mIsTexutresInited = true;
    }
    //绑定处理
    glBindTexture(GL_TEXTURE_2D, mGenTextureId);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, pixelFormat, width, height, 0, pixelFormat, GL_UNSIGNED_BYTE, (void*) (data + offset));
    mInputDataWidth = width;
    mInputDataHeight = height;
}

/**@param texturePointers 传入需要渲染处理的纹理，可以为上一次处理的结果，例如处理完后的FBOTexture **/
void RenderProgramCornerPick::loadTexture(GLuint *texturePointers, int width, int height) {
    mInputTextures = texturePointers;
    mInputTexturesWidth = width;
    mInputTexturesHeight = height;
}

/**@param outputFBOPointer 绘制到哪个framebuffer，系统默认一般为0 **/
void RenderProgramCornerPick::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {
    if (mIsDestroyed) {
        return;
    }
    glUseProgram(mCornerPickProgram.programHandle);
    //设置视窗大小及位置
    glBindFramebuffer(GL_FRAMEBUFFER, outputFBOPointer);
    glViewport(0, 0, fboW, fboH);
    //传入位置信息
    locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
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
        //设置图像分辨率
        float resolution[2];

        switch (drawType) {
            case OPENGL_VIDEO_RENDERER::RenderProgram::DRAW_DATA:
                //设置图像分辨率
                resolution[0] = mInputDataWidth;
                resolution[1] = mInputDataHeight;
                glUniform2fv(mResoulutionPointer, 1, resolution);
                glActiveTexture(GL_TEXTURE0); //激活0号纹理
                glBindTexture(GL_TEXTURE_2D, mGenTextureId); //0号纹理绑定内容
                glUniform1i(glGetUniformLocation(mCornerPickProgram.programHandle, "textureFBO"), 0); //映射到渲染脚本，获取纹理属性的指针
                break;
            case OPENGL_VIDEO_RENDERER::RenderProgram::DRAW_TEXTURE:
                //设置图像分辨率
                resolution[0] = mInputTexturesWidth;
                resolution[1] = mInputTexturesHeight;
                glUniform2fv(mResoulutionPointer, 1, resolution);
                glActiveTexture(GL_TEXTURE0); //激活0号纹理
                glBindTexture(GL_TEXTURE_2D, mInputTextures[0]); //0号纹理绑定内容
                glUniform1i(glGetUniformLocation(mCornerPickProgram.programHandle, "textureFBO"), 0); //映射到渲染脚本，获取纹理属性的指针
                break;
        }

        glDrawArrays(GL_TRIANGLE_STRIP, 0, /*mPointBufferPos / 3*/ 4); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        glDisableVertexAttribArray(mObjectPositionPointer);
        glDisableVertexAttribArray(mObjectVertColorArrayPointer);
        glDisableVertexAttribArray(mVTexCoordPointer);
    }
}

void RenderProgramCornerPick::destroy() {
    if (!mIsDestroyed) {
        //释放纹理所占用的显存
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, 0, 0, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, nullptr);
        glDeleteTextures(1, texturePointers); //销毁纹理,gen和delete要成对出现
        //删除不用的shaderprogram
        destroyProgram(mCornerPickProgram);
    }
    mIsDestroyed = true;
}