//
// Created by jiezhuchen on 2021/6/21.
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

#include <string.h>
#include <jni.h>
#include "RenderProgramOESTexture.h"
#include "android/log.h"


using namespace OPENGL_VIDEO_RENDERER;
static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

RenderProgramOESTexture::RenderProgramOESTexture() {
    vertShader = GL_SHADER_STRING(
            \n
            uniform mat4 uMVPMatrix; //旋转平移缩放 总变换矩阵。物体矩阵乘以它即可产生变换
            attribute vec3 objectPosition; //物体位置向量，参与运算但不输出给片源

            attribute vec4 objectColor; //物理颜色向量
            attribute vec2 vTexCoord; //纹理内坐标
            varying vec4 fragObjectColor;//输出处理后的颜色值给片元程序
            varying vec2 fragVTexCoord;//输出处理后的纹理内坐标给片元程序

            void main() {
                    vec2 temp = vec2(1.0, 1.0);
                    gl_Position = uMVPMatrix * vec4(objectPosition, 1.0); //设置物体位置
                    fragVTexCoord = vTexCoord; //默认无任何处理，直接输出物理内采样坐标
                    fragObjectColor = objectColor; //默认无任何处理，输出颜色值到片源
            }
    );
    fragShader = GL_SHADER_STRING(
            $#extension GL_OES_EGL_image_external : require\n
            precision highp float;
            uniform samplerExternalOES oesTexture;//OES形式的纹理输入
            uniform int funChoice;
            uniform float frame;//第几帧
            uniform float brightness;//亮度
            uniform float contrast;//对比度
            uniform vec3 rgbWeight; //白平衡
            uniform vec2 resolution;//容器的分辨率
            uniform vec2 videoResolution;//视频自身的分辨率
            varying vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            varying vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序

            float fakeRandom(vec2 st) {
                return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123 * frame / 1000.0);
            }

            //添加噪声进行测试
            vec3 getNoise(vec2 st) {
                float rnd = fakeRandom(st);
                return vec3(rnd);
            }

            void main() {
                vec2 xy = vec2(fragVTexCoord.s, fragVTexCoord.t);
                vec3 rgbWithBrightness = texture2D(oesTexture, xy).rgb * rgbWeight + brightness; //亮度调节
                vec3 rgbWithContrast = rgbWithBrightness + (rgbWithBrightness - 0.5) * contrast / 1.0;  //对比度调整 参考https://blog.csdn.net/yuhengyue/article/details/103856476
                //gl_FragColor = vec4(rgbWithContrast, fragObjectColor.a);
                //cjztest 噪声测试
                gl_FragColor = vec4(getNoise(fragVTexCoord) * rgbWithContrast.rgb, 1.0);
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

RenderProgramOESTexture::~RenderProgramOESTexture() {
    destroy();
}

void RenderProgramOESTexture::createRender(float x, float y, float z, float w, float h, int windowW,
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
    mImageProgram = createProgram(vertShader + 1, fragShader + 1);  //cjztest 测试原因屏蔽：屏蔽了依然出现花屏
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
    //亮度指针
    mBrightnessPointer = glGetUniformLocation(mImageProgram.programHandle, "brightness");
    //对比度指针
    mContrastPointer = glGetUniformLocation(mImageProgram.programHandle, "contrast");
    //白平衡指针
    mRGBWeightPointer = glGetUniformLocation(mImageProgram.programHandle, "rgbWeight");
    //设置分辨率指针，告诉gl脚本现在的分辨率
    mResoulutionPointer = glGetUniformLocation(mImageProgram.programHandle, "resolution");
}

void RenderProgramOESTexture::setAlpha(float alpha) {
    if (mColorBuf != nullptr) {
        for (int i = 3; i < sizeof(mColorBuf) / sizeof(float); i += 4) {
            mColorBuf[i] = alpha;
        }
    }
}

void RenderProgramOESTexture::setBrightness(float brightness) {
    mBrightness = brightness;
}

void RenderProgramOESTexture::setContrast(float contrast) {
    mContrast = contrast;
}

void RenderProgramOESTexture::setWhiteBalance(float redWeight, float greenWeight, float blueWeight) {
    mRedWeight = redWeight;
    mGreenWeight = greenWeight;
    mBlueWeight = blueWeight;
}

void RenderProgramOESTexture::loadData(char *data, int width, int height, int pixelFormat, int offset) {
    //不用实现
}

/**@param texturePointers 传入需要渲染处理的纹理，可以为上一次处理的结果，例如处理完后的FBOTexture **/
void RenderProgramOESTexture::loadTexture(Textures textures[]) {
    mInputTexturesArray = textures[0].texturePointers;
    mInputTextureWidth = textures[0].width;
    mInputTextureHeight = textures[0].height;
}

/**@param outputFBOPointer 绘制到哪个framebuffer，系统默认一般为0 **/
void RenderProgramOESTexture::drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) {
    if (mIsDestroyed) {
        return;
    }
    glUseProgram(mImageProgram.programHandle);
    glUniform1f(mBrightnessPointer, mBrightness);
    glUniform1f(mContrastPointer, mContrast);
    float whiteBalanceWeight[3] = {mRedWeight, mGreenWeight, mBlueWeight};
    glUniform3fv(mRGBWeightPointer, 1, whiteBalanceWeight);
    //设置视窗大小及位置
    glBindFramebuffer(GL_FRAMEBUFFER, outputFBOPointer);
    glViewport(0, 0, mWindowW, mWindowH);
    glUniform1i(mGLFunChoicePointer, 1);
    glUniform1f(mFrameCountPointer, mframeCount++);
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
//                glBindTexture(36197, mInputTexturesArray); //0号纹理绑定内容
                glBindTexture(GL_TEXTURE_2D, mInputTexturesArray); //0号纹理绑定内容，发现使用GL_TEXTURE_2D也可以绑定OES纹理
                glUniform1i(glGetUniformLocation(mImageProgram.programHandle, "oesTexture"), 0); //映射到渲染脚本，获取纹理属性的指针
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

void RenderProgramOESTexture::destroy() {
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