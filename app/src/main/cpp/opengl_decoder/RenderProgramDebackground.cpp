//
// Created by jiezhuchen on 2021/9/6.
//降噪渲染器
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <string.h>
#include <jni.h>
#include "RenderProgramDebackground.h"
#include "android/log.h"


using namespace OPENGL_VIDEO_RENDERER;
static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

RenderProgramDebackground::RenderProgramDebackground() {
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
            uniform sampler2D sTextureReplace;//需要替换的背景颜色输入
            uniform int sTextureReplaceLen; //替换颜色表的长度
            uniform sampler2D sTextureCover;//被替换的位置使用该纹理对应采样坐标位置像素替换


            uniform int funChoice;
            uniform float frame;//第几帧
            uniform vec2 resolution;//分辨率
            in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
            out vec4 fragColor;//输出到的片元颜色

            void main() {
                //todo 模式1 输入传入的纹理的颜色如果为指定的替换颜色，则fragColor采样为为纹理2对应位置的像素
                //todo 模式2 传入的纹理采样过程中对比背景纹理，如果像素颜色一样，替换为纹理2
                //原纹理颜色获取
                vec4 color = texture(sTexture, fragVTexCoord);  //采样纹理中对应坐标颜色，进行纹理渲染
                vec4 color2 = vec4(color.r, color.g, color.b, color.a);
                //根据颜色替换表替换颜色
                bool haveReplaceColorInTexture = false;
                for (int i = 0; i < sTextureReplaceLen; i++) {
                    vec2 loc = vec2(i / sTextureReplaceLen, 0.0);
                    vec4 replaceColor = texture(sTextureReplace, loc);
                    //cjztest
                    fragColor = replaceColor;
//                    if (color2.b == replaceColor.b) {
//                        haveReplaceColorInTexture = true;
//                        break;
//                    }
                }
//                if (haveReplaceColorInTexture) {
//                    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
//                } else {
//                    fragColor = color2;
//                }
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
    mTextureReplaceColorTable[0] = -1;
}

RenderProgramDebackground::~RenderProgramDebackground() {
    //cjzmark todo 记得释放资源
}

void RenderProgramDebackground::createRender(float x, float y, float z, float w, float h, int windowW, int windowH) {
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

void RenderProgramDebackground::loadTexture(Textures textures[]) {
    mInputTexturesArray = textures[0].texturePointers;
    mInputTextureWidth = textures[0].width;
    mInputTextureHeight = textures[0].height;
}

void RenderProgramDebackground::loadData(char *data, int width, int height, int pixelFormat, int offset) { //yuvl类数据忽略pixelFormat，根据mYuvKind进行判断

}

//todo
void RenderProgramDebackground::setAlpha(float alpha) {

}

//todo
void RenderProgramDebackground::setBrightness(float brightness) {

}

//todo
void RenderProgramDebackground::setContrast(float contrast) {

}

//todo
void RenderProgramDebackground::setWhiteBalance(float redWeight, float greenWeight, float blueWeight) {

}

void RenderProgramDebackground::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {
    if (mIsDestroyed) {
        return;
    }
    glUseProgram(mImageProgram.programHandle);
    //设置视窗大小及位置
    glBindFramebuffer(GL_FRAMEBUFFER, outputFBOPointer);
    glViewport(0, 0, mWindowW, mWindowH);
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
                //输入纹理
                glActiveTexture(GL_TEXTURE0); //激活0号纹理
                glBindTexture(GL_TEXTURE_2D, mInputTexturesArray);
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTexture"),
                            0); //映射到渲染脚本，获取纹理属性的指针
                resolution[0] = (float) mInputTextureWidth;
                resolution[1] = (float) mInputTextureHeight;
                glUniform2fv(mResoulutionPointer, 1, resolution);





                //输入替换颜色表:
                    //如果还没创建颜色替换纹理：
                if (mTextureReplaceColorTable[0] == -1) {
                    int cjztestColorTable[255];
                    for (int i = 0; i < 256; i++) {
                        cjztestColorTable[i] = (int) (0xFF000000 | (i));
                    }
                    glGenTextures(1, mTextureReplaceColorTable);
                    glBindTexture(GL_TEXTURE_2D, mTextureReplaceColorTable[0]);
                    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, cjztestColorTable); //因为这里使用了双字节，所以纹理大小对比使用单字节的Y通道纹理，宽度首先要缩小一般，而uv层高度本来就只有y层一般，所以高度也除以2
                }
                glActiveTexture(GL_TEXTURE1); //激活1号纹理
                glBindTexture(GL_TEXTURE_2D, mTextureReplaceColorTable[0]);
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTextureReplace"),
                            1); //映射到渲染脚本，获取纹理属性的指针
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTextureReplaceLen"),
                            1); //输入替换纹理的长度




                break;
        }
        glDrawArrays(GL_TRIANGLE_STRIP, 0, /*mPointBufferPos / 3*/ 4); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        glDisableVertexAttribArray(mObjectPositionPointer);
        glDisableVertexAttribArray(mObjectVertColorArrayPointer);
        glDisableVertexAttribArray(mVTexCoordPointer);
    }
}

void RenderProgramDebackground::destroy() {

}

void RenderProgramDebackground::debackgroundColor(int colorList[]) {

}
