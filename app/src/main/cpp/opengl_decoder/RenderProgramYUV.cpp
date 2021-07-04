//
// Created by jiezhuchen on 2021/6/21.
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/native_window.h>
//#include <ui/GraphicBuffer.h>
#include <dlfcn.h>
#include "shaderUtil.c"
#include "RenderProgramYUV.h"

#include <string.h>
#include <jni.h>

#include "android/log.h"

static const char *TAG = "nativeGL";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

using namespace OPENGL_VIDEO_RENDERER;

gLslHandle mYuvBufferDrawProgram;
int mGenYTextureId = 0;
int mGenUVTextureId = 0;
enum YuvKinds {
    YUV_420SP_UVUV,
    YUV_420SP_VUVU,
    YUV_420P_UUVV,
    YUV_420P_VVUU,
};
/**设定YUV类型**/
YuvKinds mYuvKinds = YUV_420SP_UVUV;
int mImgPanelYByteSize;
int mImgPanelUVByteSize;
bool mIsFirstFrame = true;
GLuint mYPanelPixelBuffferPointerArray[2];
GLuint mUVPanelPixelBuffferPointerArray[2];
GLint mGLFrameObjectPositionPointer;
GLint mGLFrameBufferProgramFunChoicePointer;
GLint mGLFrameVTexCoordPointer;
GLint mGLFrameObjectVertColorArrayPointer;
GLint mGLFrameuMVPMatrixPointer;
GLint mFrameCountPointer;
GLint mResoulutionPointer;
float texCoor[]   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
        {
                1.0, 0.0,
                0.0, 0.0,
                1.0, 1.0,
                0.0, 1.0
        };
float mVert[4 * 3];
bool mIsDestroyed = false;

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

void createEmptyTexture(int *texturePointers, int imgWidth, int imgHeight, int pixelFormat) {
    glGenTextures(1, (GLuint*) texturePointers); //只要值不重复即可
    //UV纹理初始化
    glBindTexture(GL_TEXTURE_2D, texturePointers[0]);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
    glTexImage2D(GL_TEXTURE_2D, 0, pixelFormat, imgWidth, imgHeight, 0, pixelFormat, GL_UNSIGNED_BYTE, nullptr); //因为这里使用了双字节，所以纹理大小对比使用单字节的Y通道纹理，宽度首先要缩小一般，而uv层高度本来就只有y层一般，所以高度也除以2
}

/**
 * 创建2个PBO提高纹理更新的速度
 **/
void createPBO(int imgWidth, int imgHeight) {
    //创建Y通道PBO
    mImgPanelYByteSize = imgWidth * imgHeight;
    glGenBuffers(2, mYPanelPixelBuffferPointerArray);

    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, mYPanelPixelBuffferPointerArray[0]);
    glBufferData(GL_PIXEL_UNPACK_BUFFER, mImgPanelYByteSize, nullptr, GL_STREAM_DRAW);

    glBindBuffer(GL_PIXEL_PACK_BUFFER, mYPanelPixelBuffferPointerArray[1]);
    glBufferData(GL_PIXEL_PACK_BUFFER, mImgPanelYByteSize,  nullptr, GL_STREAM_DRAW);
    //创建UV通道PBO
    mImgPanelUVByteSize = imgWidth * imgHeight / 2;
    mUVPanelPixelBuffferPointerArray = new int[2];
    glGenBuffers(2, mUVPanelPixelBuffferPointerArray);

    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[0]);
    glBufferData(GL_PIXEL_UNPACK_BUFFER, mImgPanelUVByteSize,  nullptr, GL_STREAM_DRAW);

    glBindBuffer(GL_PIXEL_PACK_BUFFER, mUVPanelPixelBuffferPointerArray[1]);
    glBufferData(GL_PIXEL_PACK_BUFFER, mImgPanelUVByteSize,  nullptr, GL_STREAM_DRAW);
    //初始化完重新绑定默认buffer，否则可能影响其他图形
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
}

void RenderProgramYUV::createRender() {
    mYuvBufferDrawProgram = createProgram(vertShader, fragShader);
    mYuvKinds = YUV_420SP_UVUV;

    //获取程序中顶点位置属性引用"指针"
    mGLFrameObjectPositionPointer = glGetAttribLocation(mYuvBufferDrawProgram.programHandle, "objectPosition");
    //渲染方式选择
    mGLFrameBufferProgramFunChoicePointer = glGetUniformLocation(mYuvBufferDrawProgram.programHandle, "funChoice");
    //纹理采样坐标
    mGLFrameVTexCoordPointer = glGetAttribLocation(mYuvBufferDrawProgram.programHandle, "vTexCoord");
    //获取程序中顶点颜色属性引用"指针"
    mGLFrameObjectVertColorArrayPointer = glGetAttribLocation(mYuvBufferDrawProgram.programHandle, "objectColor");
    //获取程序中总变换矩阵引用"指针"
    mGLFrameuMVPMatrixPointer = glGetUniformLocation(mYuvBufferDrawProgram.programHandle, "uMVPMatrix");
    //渲染帧计数指针
    mFrameCountPointer = glGetUniformLocation(mYuvBufferDrawProgram.programHandle, "frame");
    //设置分辨率指针，告诉gl脚本现在的分辨率
    mResoulutionPointer = glGetUniformLocation(mYuvBufferDrawProgram.programHandle, "resolution");


}

void refreshBuffer(char *imgBytes, int mImgWidth, int mImgHeight) {
    //更新ypanel数据
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mGenYTextureId);
    glUniform1i(glGetUniformLocation(mYuvBufferDrawProgram.programHandle, "textureY"), 0); //获取纹理属性的指针
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, mYPanelPixelBuffferPointerArray[mFrameCount % 2]);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, mImgWidth, mImgHeight, GL_LUMINANCE, GL_UNSIGNED_BYTE, null); //1字节为一个单位
    //更新图像数据，复制到 PBO 中
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, mYPanelPixelBuffferPointerArray[(mFrameCount + 1) % 2]);
    glBufferData(GL_PIXEL_UNPACK_BUFFER, mImgPanelYByteSize, null, GL_STREAM_DRAW);
    Buffer buf = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, mImgPanelYByteSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
    //填充像素
    ByteBuffer bytebuffer = ((ByteBuffer) buf).order(ByteOrder.nativeOrder());
    bytebuffer.position(0);
    bytebuffer.put(imgBytes, 0, mImgWidth * mImgHeight);
    bytebuffer.position(0);
    glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    //更新uvpanel数据
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, mGenUVTextureId);
    glUniform1i(glGetUniformLocation(mYuvBufferDrawProgram.programHandle, "textureUV"), 1); //获取纹理属性的指针
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[mFrameCount % 2]);
    switch (mYuvKinds) {
        default:
        case YUV_420SP_UVUV:
        case YUV_420SP_VUVU:
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, mImgWidth / 2, mImgHeight / 2, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, null); //2字节为一个单位，所以宽度因为单位为2字节一个，对比1字节时直接对半
            break;
        case YUV_420P_UUVV:
        case YUV_420P_VVUU:
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, mImgWidth, mImgHeight / 2, GL_LUMINANCE, GL_UNSIGNED_BYTE, null);
            break;
    }
    //更新图像数据，复制到 PBO 中
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[(mFrameCount + 1) % 2]);
    glBufferData(GL_PIXEL_UNPACK_BUFFER, mImgPanelUVByteSize, null, GL_STREAM_DRAW);
    buf = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, mImgPanelUVByteSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
    //填充像素
    bytebuffer = ((ByteBuffer) buf).order(ByteOrder.nativeOrder());
    bytebuffer.position(0);
    bytebuffer.put(imgBytes, mImgWidth * mImgHeight, mImgWidth * mImgHeight / 2);
    bytebuffer.position(0);
    glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
}


/**绘制画面到framebuffer**/
void drawToFrameBuffer(int outputFBOTexturePointer, float cameraMatrix[], float projMatrix[]) {
    if (mIsDestroyed) {
        return;
    }
    glUseProgram(mYuvBufferDrawProgram.programHandle);
    //设置视窗大小及位置
    glViewport(0, 0, mFrameBufferWidth, mFrameBufferHeight);
    //绑定帧缓冲id
    if (mFrameCount % 2 == 0) {
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBufferPointerArray[0]);
    } else {
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBufferPointerArray[1]);
    }
    //清除深度缓冲与颜色缓冲
    if (!mFrameBufferClean && !mFrameBufferCleanOnce) { //实现渲染画面叠加
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        mFrameBufferCleanOnce = true;
    }
    if (mFrameBufferClean) {
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glUniform1i(mGLFrameBufferProgramFunChoicePointer, -1); //第一次加载选择纹理方式渲染
    }
    //设置它的坐标系
    locationTrans(cameraMatrix, projMatrix, this.mGLFrameuMVPMatrixPointer);
    //设置图像分辨率
    glUniform2fv(mResoulutionPointer, 1, new float[]{mWindowW, mWindowH}, 0);
    locationTrans(cameraMatrix, projMatrix, this.mGLFrameuMVPMatrixPointer);
    if (mPointBuf != null && mColorBuf != null) {
        if (mFrameCount < 0) {
            mFrameCount = 0;
        }
        mPointBuf.position(0);
        mColorBuf.position(0);
//            glUniform1i(glGetUniformLocation(mYuvBufferDrawProgram, "sTexture"), 0); //获取纹理属性的指针
        //将顶点位置数据送入渲染管线
        glVertexAttribPointer(mGLFrameObjectPositionPointer, 3, GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
        //将顶点颜色数据送入渲染管线
        glVertexAttribPointer(mGLFrameObjectVertColorArrayPointer, 4, GL_FLOAT, false, 0, mColorBuf);
        //将顶点纹理坐标数据传送进渲染管线
        glVertexAttribPointer(mGLFrameVTexCoordPointer, 2, GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
        glEnableVertexAttribArray(mGLFrameObjectPositionPointer); //启用顶点属性
        glEnableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);  //启用颜色属性
        glEnableVertexAttribArray(mGLFrameVTexCoordPointer);  //启用纹理采样定位坐标
//            glActiveTexture(GL_TEXTURE0);
        //绘制yuv
        switch (mYuvKinds) {
            default:
            case YUV_420SP_UVUV:
                glUniform1i(mGLFrameBufferProgramFunChoicePointer, 0);
                break;
            case YUV_420SP_VUVU:
                glUniform1i(mGLFrameBufferProgramFunChoicePointer, 1);
                break;
            case YUV_420P_UUVV:
                glUniform1i(mGLFrameBufferProgramFunChoicePointer, 2);
                break;
            case YUV_420P_VVUU:
                glUniform1i(mGLFrameBufferProgramFunChoicePointer, 3);
                break;
        }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mGenYTextureId);
        glUniform1i(glGetUniformLocation(mYuvBufferDrawProgram, "textureY"), 0); //获取纹理属性的指针

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, mGenUVTextureId);
        glUniform1i(glGetUniformLocation(mYuvBufferDrawProgram, "textureUV"), 1); //获取纹理属性的指针

        glDrawArrays(GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        glDisableVertexAttribArray(mGLFrameObjectPositionPointer);
        glDisableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);
        glDisableVertexAttribArray(mGLFrameVTexCoordPointer);
    }
    //绑会系统默认framebuffer，否则会显示不出东西
    glBindFramebuffer(GL_FRAMEBUFFER, 0);//绑定帧缓冲id
}

/**YUV格式如果使用opengles 3.0的话，不需要在此时使用glTexImage2d，更建议在更新纹理的接口中使用PBO更新**/
void RenderProgramYUV::drawData(float *cameraMatrix, float *projMatrix, int outputFBOTexturePointer, char *data, int width, int height, int pixelFormat, int offset) {
    if (mIsFirstFrame) {
        //生成textureY纹理
        int mGenYTexutreArray[1];
        createEmptyTexture(mGenYTexutreArray, width, width, GL_LUMINANCE);
        mGenYTextureId = mGenYTexutreArray[0];
        //生成textureUV纹理
        int mGenUVTexutreArray[1];
        switch (mYuvKinds) {
            default:
            case YUV_420SP_UVUV:
            case YUV_420SP_VUVU:
                createEmptyTexture(mGenUVTexutreArray, width / 2, height / 2, GL_LUMINANCE_ALPHA);
                break;
            case YUV_420P_UUVV:
            case YUV_420P_VVUU:
                createEmptyTexture(mGenUVTexutreArray, width, height / 2, GL_LUMINANCE);
                break;
        }
        mGenUVTextureId = mGenUVTexutreArray[0];
        createPBO(width, height);
    }
    refreshBuffer(data, width, height);
    drawToFrameBuffer(outputFBOTexturePointer, cameraMatrix, projMatrix);
    mIsFirstFrame = false;
}