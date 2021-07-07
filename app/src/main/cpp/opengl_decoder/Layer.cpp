//
// Created by jiezhuchen on 2021/7/5.
//

#include <GLES3/gl3.h>
#include "Layer.h"
#include "RenderProgram.h"
#include "shaderUtil.h"

using namespace OPENGL_VIDEO_RENDERER;


Layer::Layer(float x, float y, float z, float w, float h, int windowW, int windowH) {
    mX = x;
    mY = y;
    mZ = z;
    mWidth = w;
    mHeight = h;
    mWindowW = windowW;
    mWindowH = windowH;
    mRenderSrcData.data = nullptr;
    createFrameBuffer();
    createLayerProgram();
}

Layer::~Layer() {
    destroy();
}

void Layer::initObjMatrix() {
    //创建单位矩阵
    setIdentityM(mObjectMatrix, 0);
}

void Layer::scale(float sx, float sy, float sz) {
    scaleM(mObjectMatrix, 0, sx, sy, sz);
}

void Layer::translate(float dx, float dy, float dz) {
    translateM(mObjectMatrix, 0, dx, dy, dz);
}

void Layer::rotate(int degree, float roundX, float roundY, float roundZ) {
    rotateM(mObjectMatrix, 0, degree, roundX, roundY, roundZ);
}

void Layer::locationTrans(float cameraMatrix[], float projMatrix[], int muMVPMatrixPointer) {
    multiplyMM(mMVPMatrix, 0, cameraMatrix, 0, mObjectMatrix, 0);         //将摄像机矩阵乘以物体矩阵
    multiplyMM(mMVPMatrix, 0, projMatrix, 0, mMVPMatrix, 0);         //将投影矩阵乘以上一步的结果矩阵
    glUniformMatrix4fv(muMVPMatrixPointer, 1, false, mMVPMatrix);        //将最终变换关系传入渲染管线
}

//todo 每一次修改都会导致绑定的纹理本身被修改，这样会导致循环论证一样的问题，所以要使用双Framebuffer
void Layer::createFrameBuffer() {
    int frameBufferCount = sizeof(mFrameBufferPointerArray) / sizeof(GLuint);

    //生成framebuffer
    glGenFramebuffers(frameBufferCount, mFrameBufferPointerArray);

    //生成渲染缓冲buffer
    glGenRenderbuffers(frameBufferCount, mRenderBufferPointerArray);

    //生成framebuffer纹理pointer
    glGenTextures(frameBufferCount, mFrameBufferTexturePointerArray);

    //遍历framebuffer并初始化
    for (int i = 0; i < frameBufferCount; i++) {
        //绑定帧缓冲，遍历两个framebuffer分别初始化
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBufferPointerArray[i]);
        //绑定缓冲pointer
        glBindRenderbuffer(GL_RENDERBUFFER, mRenderBufferPointerArray[i]);
        //为渲染缓冲初始化存储，分配显存
        glRenderbufferStorage(GL_RENDERBUFFER,
                GL_DEPTH_COMPONENT16, mWindowW, mWindowH); //设置framebuffer的长宽

        glBindTexture(GL_TEXTURE_2D, mFrameBufferTexturePointerArray[i]); //绑定纹理Pointer

        glTexParameterf(GL_TEXTURE_2D,//设置MIN采样方式
                GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D,//设置MAG采样方式
                GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D,//设置S轴拉伸方式
                GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D,//设置T轴拉伸方式
                GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D//设置颜色附件纹理图的格式
                (
                        GL_TEXTURE_2D,
                0,                        //层次
                GL_RGBA,        //内部格式
                mWindowW,            //宽度
                mWindowH,            //高度
                0,                        //边界宽度
                GL_RGBA,            //格式
                GL_UNSIGNED_BYTE,//每个像素数据格式
                nullptr
        );
        glFramebufferTexture2D        //设置自定义帧缓冲的颜色缓冲附件
                (
                        GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,    //颜色缓冲附件
                GL_TEXTURE_2D,
                mFrameBufferTexturePointerArray[i],                        //纹理id
                0                                //层次
        );
        glFramebufferRenderbuffer    //设置自定义帧缓冲的深度缓冲附件
                (
                        GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,        //深度缓冲附件
                GL_RENDERBUFFER,            //渲染缓冲
                mRenderBufferPointerArray[i]                //渲染深度缓冲id
        );
    }
    //绑回系统默认framebuffer，否则会显示不出东西
    glBindFramebuffer(GL_FRAMEBUFFER, 0);//绑定帧缓冲id
}

void Layer::createLayerProgram() {
    char vertShader[] = GL_SHADER_STRING(
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
    char fragShader[] = GL_SHADER_STRING(
            ##version 300 es\n
            precision highp float;
            uniform sampler2D textureFBO;//纹理输入
            in vec4 fragObjectColor;//接收vertShader处理后的颜色值给片元程序
            in vec2 fragVTexCoord;//接收vertShader处理后的纹理内坐标给片元程序
            out vec4 fragColor;//输出到的片元颜色

            void main() {
                    vec4 color = texture(textureFBO, fragVTexCoord);//采样纹理中对应坐标颜色，进行纹理渲染
                    color.a = color.a * fragObjectColor.a;//利用顶点透明度信息控制纹理透明度
                    fragColor = color;
            }
    );
    float ratio = (float) mWindowH / mWindowW;;
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
    float vertxData[] = {
            mX + 2, mY, mZ,
            mX, mY, mZ,
            mX + 2, mY + ratio * 2, mZ,
            mX, mY + ratio * 2, mZ,
    };
    memcpy(mVertxData, vertxData, sizeof(vertxData));
    mLayerProgram = createProgram(vertShader + 1, fragShader + 1);
    //获取程序中顶点位置属性引用"指针"
    mObjectPositionPointer = glGetAttribLocation(mLayerProgram.programHandle, "objectPosition");
    //纹理采样坐标
    mVTexCoordPointer = glGetAttribLocation(mLayerProgram.programHandle, "vTexCoord");
    //获取程序中顶点颜色属性引用"指针"
    mObjectVertColorArrayPointer = glGetAttribLocation(mLayerProgram.programHandle, "objectColor");
    //获取程序中总变换矩阵引用"指针"
    muMVPMatrixPointer = glGetUniformLocation(mLayerProgram.programHandle, "uMVPMatrix");
    //创建单位矩阵
    initObjMatrix();
}

void Layer::destroy() {
    //todo
}

void Layer::addRenderProgram(RenderProgram *program) {
    mRenderProgramList.push_back(program);
}

/**给每个模板传入渲染数据**/
void Layer::loadData(char *data, int width, int height, int pixelFormat, int offset) {
    mRenderSrcData.data = data;
    mRenderSrcData.width = width;
    mRenderSrcData.height = height;
    mRenderSrcData.pixelFormat = pixelFormat;
    mRenderSrcData.offset = offset;
}

void Layer::drawLayerToFrameBuffer(float *cameraMatrix, float *projMatrix, GLuint outputFBOPointer) {
    glUseProgram(mLayerProgram.programHandle);
    glBindFramebuffer(GL_FRAMEBUFFER, outputFBOPointer);
    locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
    /**实现两个Framebuffer的画面叠加，这里解释一下：
     * 如果是偶数个渲染器，那么在交替渲染之后，那么第0个FBO的画面是上一个画面，第1个FBO为最新画面，所以要先绘制第0个FBO内容再叠加第一个
     * 否则则是交替后，第1个渲染器是上个画面，第0个FBO是上一个画面，叠加顺序则要进行更改**/
    for(int i = 0; i < 2; i ++) {
        glActiveTexture(GL_TEXTURE0);
        if (mRenderProgramList.size() % 2 == 0) {
            glBindTexture(GL_TEXTURE_2D, mFrameBufferTexturePointerArray[i]);
        } else {
            glBindTexture(GL_TEXTURE_2D, mFrameBufferTexturePointerArray[1 - i]);
        }
        glUniform1i(glGetUniformLocation(mLayerProgram.programHandle, "textureFBO"), 0); //获取纹理属性的指针
        //将顶点位置数据送入渲染管线
        glVertexAttribPointer(mObjectPositionPointer, 3, GL_FLOAT, false, 0, mVertxData); //三维向量，size为2
        //将顶点颜色数据送入渲染管线
        glVertexAttribPointer(mObjectVertColorArrayPointer, 4, GL_FLOAT, false, 0, mColorBuf);
        //将顶点纹理坐标数据传送进渲染管线
        glVertexAttribPointer(mVTexCoordPointer, 2, GL_FLOAT, false, 0, mTexCoor);  //二维向量，size为2
        glEnableVertexAttribArray(mObjectPositionPointer); //启用顶点属性
        glEnableVertexAttribArray(mObjectVertColorArrayPointer);  //启用颜色属性
        glEnableVertexAttribArray(mVTexCoordPointer);  //启用纹理采样定位坐标
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        glDisableVertexAttribArray(mObjectPositionPointer);
        glDisableVertexAttribArray(mObjectVertColorArrayPointer);
        glDisableVertexAttribArray(mVTexCoordPointer);
    }
}

/**逐步加工绘制**/
void
Layer::drawTo(float *cameraMatrix, float *projMatrix, GLuint outputFBOPointer, int fboW, int fboH) {
    glBindFramebuffer(1, mFrameBufferPointerArray[0]);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
    glBindFramebuffer(1, mFrameBufferPointerArray[1]);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT); //清理屏幕
    int i = 0;
    /**第0个渲染器以data为数据输入，使用FBO[0]渲染结果。第1个渲染器使用FBO_texture[0]作为纹理输入，渲染结果输出到FBO[1]。
     * 第2个渲染器使用FBO_texture[1]作为纹理输入，渲染结果输出到FBO[0]，依次循环互换结果和输入，实现效果叠加。
     * 使用双FBO互为绑定的原因是为了解决部分shader算法如果绑定的FBO_texture和输出的FBO是同一个将会出现异常，所以使用此方法**/
    for (auto item = mRenderProgramList.begin(); item != mRenderProgramList.end(); item++, i++) {
        //接收绘制数据的framebuffer和作为纹理输入使用的framebuffer不能是同一个
        int fbo = i % 2 == 0 ? mFrameBufferPointerArray[0] : mFrameBufferPointerArray[1];
        int fboTexture = i % 2 == 1 ? mFrameBufferTexturePointerArray[0] : mFrameBufferTexturePointerArray[1];
        //第一个渲染器接受图层原始数据，其他的从上一个渲染结果中作为输入
        if (i == 0) {
            if (mRenderSrcData.data != nullptr) {
                (*item)->loadData(mRenderSrcData.data, mRenderSrcData.width, mRenderSrcData.height,
                                  mRenderSrcData.pixelFormat, mRenderSrcData.offset);
                //渲染器处理结果放到图层FBO中
                (*item)->drawTo(cameraMatrix, projMatrix, RenderProgram::DRAW_DATA,
                                fbo, mWindowW, mWindowH);
            }
        } else { //如果只有一个渲染器则走不到else里，否则第0个打后的渲染器依次使用上一个渲染器的结果，也就是图层FBO中的数据作为输入  bug
            //使用上一个渲染器保存到FBO的结果，也就是FBO_texture作为纹理输入进行二次处理
            GLuint t = fboTexture;
            GLuint textures[] = {t};
            (*item)->loadTexture(textures, mWindowW, mWindowH); //使用上一个渲染器的渲染结果作为绘制输入
            //渲染器处理结果放到图层FBO中
            (*item)->drawTo(cameraMatrix, projMatrix, RenderProgram::DRAW_TEXTURE,
                            fbo, mWindowW, mWindowH);
        }
    }
    //最后渲染到目标framebuffer
    drawLayerToFrameBuffer(cameraMatrix, projMatrix, outputFBOPointer);
    //渲染统计
    mFrameCount++;
}


