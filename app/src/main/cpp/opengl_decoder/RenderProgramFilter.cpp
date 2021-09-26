//
// Created by jiezhuchen on 2021/6/21.
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <string.h>
#include <jni.h>
#include <cstdlib>
#include "RenderProgramFilter.h"
#include "android/log.h"


using namespace OPENGL_VIDEO_RENDERER;
static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

RenderProgramFilter::RenderProgramFilter() {
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
//    fragShader = GL_SHADER_STRING(
//            ##version 300 es\n
//            precision highp float;
//            uniform sampler2D sTexture;//图像纹理输入
//            uniform sampler3D lutTexture;//滤镜纹理输入
//            uniform float frame;//第几帧
//            uniform vec2 resolution;//分辨率
//            in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
//            in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
//            out vec4 fragColor;//输出到的片元颜色
//
//            void main() {
//                vec4 srcColor = texture(sTexture, fragVTexCoord);
////                vec4 lutColor = texture(lutTexture, srcColor);
////                fragColor = vec4(srcColor, 1.0);
////                fragColor = vec4(fragVTexCoord, 1.0, 1.0);
//                fragColor = vec4(1.0, 0.0, 0.0, 1.0);
//            }
//    );
    fragShader = GL_SHADER_STRING(
            ##version 300 es\n
            precision highp float;
            uniform sampler2D sTexture;//图像纹理输入
            uniform sampler3D lutTexture;//滤镜纹理输入
            uniform float frame;//第几帧
            uniform vec2 resolution;//分辨率
            in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
            out vec4 fragColor;//输出到的片元颜色

            void main() {
                vec4 srcColor = texture(sTexture, fragVTexCoord);
                vec4 lutColor = texture(lutTexture, srcColor.rgb);
                fragColor = texture(lutTexture, vec3(fragVTexCoord, 0.2));
//                fragColor = vec4(lutColor.rgb, 1.0);
//                fragColor = vec4(0.5, srcColor.g, srcColor.b, 1.0);
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

RenderProgramFilter::~RenderProgramFilter() {
    destroy();
}

void RenderProgramFilter::createRender(float x, float y, float z, float w, float h, int windowW,
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

void RenderProgramFilter::setAlpha(float alpha) {
    if (mColorBuf != nullptr) {
        for (int i = 3; i < sizeof(mColorBuf) / sizeof(float); i += 4) {
            mColorBuf[i] = alpha;
        }
    }
}

//todo
void RenderProgramFilter::setBrightness(float brightness) {

}

//todo
void RenderProgramFilter::setContrast(float contrast) {

}

//todo
void RenderProgramFilter::setWhiteBalance(float redWeight, float greenWeight, float blueWeight) {

}

void RenderProgramFilter::loadData(char *data, int width, int height, int pixelFormat, int offset) {
    if (!mIsTexutresInited) {
        glUseProgram(mImageProgram.programHandle);
        glGenTextures(1, mTexturePointers);
        mGenTextureId = mTexturePointers[0];
        mIsTexutresInited = true;
    }
    //绑定处理
    glBindTexture(GL_TEXTURE_2D, mGenTextureId);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, pixelFormat, width, height, 0, pixelFormat, GL_UNSIGNED_BYTE, (void*) (data + offset));
    mDataWidth = width;
    mDataHeight = height;
}

/**@param texturePointers 传入需要渲染处理的纹理，可以为上一次处理的结果，例如处理完后的FBOTexture **/
void RenderProgramFilter::loadTexture(Textures textures[]) {
    mInputTexturesArrayPointer = textures[0].texturePointers;
    mInputTextureWidth = textures[0].width;
    mInputTextureHeight = textures[0].height;
}

/**设置LUT滤镜**/
void RenderProgramFilter::setLut(char* lutPixels, int lutWidth, int lutHeight, int unitLength) {
    int pages = 0;
    if (lutWidth == unitLength) { //竖条模式的lut
        pages = lutHeight / unitLength;
    } else if (lutWidth != unitLength && lutHeight != unitLength) { //平铺
        pages = (lutHeight / unitLength) * (lutWidth / unitLength);
    } else { //横条模式
        pages = lutWidth / unitLength;
    }
    glUseProgram(mImageProgram.programHandle);
    glGenTextures(1, mLutTexutresPointers);


//    glBindTexture(GL_TEXTURE_2D_ARRAY, mLutTexutresPointers[0]); //使用纹理数组的方式给lut滤镜分页成多个纹理，方便处理
//    glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL_RGBA, unitLength, unitLength, pages); //创建空间
//    //加载lut页，每页一个纹理:
//    if (lutWidth == unitLength) { //竖条模式的lut
//        for (int i = 0; i < pages; i++) {
//
//            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, unitLength, unitLength, 1, GL_RGBA, GL_UNSIGNED_BYTE, lutPixels + unitLength * unitLength * i * 4);
//        }
//    } else if (lutWidth != unitLength && lutHeight != unitLength) { //平铺
//        char* pixels = (char*) malloc(unitLength * unitLength * 4); //申请堆内存组合page
//        for (int i = 0; i < lutWidth / unitLength; i++) {
//            for (int j = 0; j < lutHeight / unitLength; j++) {
//                int offset = (j * unitLength * lutWidth) + (i * unitLength);
//                memcpy(pixels + j * unitLength * 4, lutPixels + offset * 4, unitLength * 4);
//            }
//            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, unitLength, unitLength, 1, GL_RGBA, GL_UNSIGNED_BYTE, lutPixels + unitLength * unitLength * i * 4);
//        }
//        free(pixels);
//    }
//    glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTexture"), 0); //映射到渲染脚本，获取纹理属性的指针


    glBindTexture(GL_TEXTURE_3D, mLutTexutresPointers[0]);
    glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    memset(lutPixels, 0xFF, 4 * lutWidth * lutHeight);  //cjztest 纯白测试
    glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA, unitLength, unitLength, unitLength, 0, GL_RGBA, GL_UNSIGNED_BYTE, lutPixels);
    glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "lutTexture"), 0); //映射到渲染脚本，获取纹理属性的指针
}


/**@param outputFBOPointer 绘制到哪个framebuffer，系统默认一般为0 **/
void RenderProgramFilter::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {
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
                glActiveTexture(GL_TEXTURE0); //激活0号纹理
                glBindTexture(GL_TEXTURE_2D, mGenTextureId); //0号纹理绑定内容
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTexture"), 0); //映射到渲染脚本，获取纹理属性的指针
                resolution[0] = (float) mDataWidth;
                resolution[1] = (float) mDataHeight;
                glUniform2fv(mResoulutionPointer, 1, resolution);
                break;
            case OPENGL_VIDEO_RENDERER::RenderProgram::DRAW_TEXTURE:
                glActiveTexture(GL_TEXTURE0); //激活0号纹理
                glBindTexture(GL_TEXTURE_2D, mInputTexturesArrayPointer); //0号纹理绑定内容
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "sTexture"), 0); //映射到渲染脚本，获取纹理属性的指针
                resolution[0] = (float) mInputTextureWidth;
                resolution[1] = (float) mInputTextureHeight;
                glUniform2fv(mResoulutionPointer, 1, resolution);
                break;
        }



        glActiveTexture(GL_TEXTURE1); //激活1号纹理
        glBindTexture(GL_TEXTURE_3D, mLutTexutresPointers[0]);
        glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "lutTexture"), 1); //映射到渲染脚本，获取纹理属性的指针




        glDrawArrays(GL_TRIANGLE_STRIP, 0, /*mPointBufferPos / 3*/ 4); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        glDisableVertexAttribArray(mObjectPositionPointer);
        glDisableVertexAttribArray(mObjectVertColorArrayPointer);
        glDisableVertexAttribArray(mVTexCoordPointer);
    }
}

void RenderProgramFilter::destroy() {
    if (!mIsDestroyed) {
        //释放纹理所占用的显存
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, 0, 0, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, nullptr);
        glDeleteTextures(1, mTexturePointers); //销毁纹理,gen和delete要成对出现
        //删除不用的shaderprogram
        destroyProgram(mImageProgram);
    }
    mIsDestroyed = true;
}