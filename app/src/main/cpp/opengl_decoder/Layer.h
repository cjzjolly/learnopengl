//
// Created by jiezhuchen on 2021/7/5.
//图层类
//

#ifndef LEARNOPENGL_LAYER_H
#define LEARNOPENGL_LAYER_H
#include "RenderProgram.h"
#include <list>
#include "shaderUtil.h"

using namespace OPENGL_VIDEO_RENDERER;


class Layer {
public:
    enum DrawType {
        DRAW_DATA,  //绘制已经load好的像素
        DRAW_TEXTURE  //绘制已经load好的纹理
    };

    /** @param x,y,z 初始化图层左上角（归一式坐标），传到objectMatrix中
         * @param w,h 图层的长和宽（归一式坐标）
         * @param windowW,windowH 渲染面实际分辨率**/
    Layer(float x, float y, float z, float w, float h, int windowW, int windowH);

    ~Layer();

    /**渲染程序模板表，可以添加多个渲染模板到表中实现图像流水线式加工**/
    void addRenderProgram(RenderProgram *program);

    /**删除涂层中指定的渲染程序**/
    void removeRenderProgram(RenderProgram *program);

    /**传入数据源**/
    /**更新显存中对应纹理地址的图像数据
 **/
    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    /**如果要绘制的东西本身就是一个纹理呢**/
    void loadTexture(GLuint texturePointer, int width, int height);

    /**绘制，遍历mRenderProgramList中的所有渲染模板
     * @param outputFBOPointer 叠加用的FBO，使得图层处理效果和内容可以层层叠加，如果需要fboTexutre本身也可以视为一个处理对象放入drawTexture中进行处理**/
    void drawTo(float *cameraMatrix, float *projMatrix, GLuint outputFBOPointer, int fboW, int fboH, DrawType drawType);

    void destroy();

    /**顶点变换矩阵 间接 缩放量，不允许用户直接操作顶点变换矩阵**/
    void setUserScale(float sx, float sy, float sz);

    void setUserRotate(float degree, float vecX, float vecY, float vecZ);

private:
    void createFrameBuffer();
    void createLayerProgram();
    /**把物品顶点变换矩阵初始化为单位矩阵。**/
    void initObjMatrix();
    void drawLayerToFrameBuffer(float *cameraMatrix, float *projMatrix, GLuint outputFBOPointer, DrawType drawType);
    /**顶点变换矩阵 乘以 缩放量**/
    void scale(float sx, float sy, float sz);
    void translate(float dx, float dy, float dz);
    void rotate(int degree,float roundX, float roundY, float roundZ);

    /**传入shaderProgram的最终场景控制指针(ID)，把变换处理后的物品坐标传到muMVPMatrixPointer，
     * 整个场景的控制则由cameraMatrix摄像机矩阵和projMatrix投影矩阵控制
     * @param cameraMatrix 摄像机矩阵，决定你用什么角度和方式看东西
     * @param 决定物体怎么投影到视点
     * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染**/
    void locationTrans(float cameraMatrix[], float projMatrix[], int muMVPMatrixPointer);

    /**渲染程序模板表，可以添加多个渲染模板到表中实现图像流水线式加工**/
    std::list<RenderProgram*> mRenderProgramList;
    float mX;
    float mY;
    float mZ;
    float mWidth;
    float mHeight;
    int mWindowW;
    int mWindowH;
    struct RenderSrcData {
        char* data;
        int width;
        int height;
        int pixelFormat;
        int offset;
    };
    struct RenderSrcTexture {
        GLuint texturePointer;
        int width;
        int height;
    };
    struct RenderSrcData mRenderSrcData;
    struct RenderSrcTexture mRenderSrcTexture;

    float mObjectMatrix[16];    //具体物体的3D变换矩阵，包括旋转、平移、缩放
    float mUserObjectMatrix[16];    //用户自定义的3D变换矩阵，包括旋转、平移、缩放
    float mUserObjectRotateMatrix[16];    //用户自定义的3D变换矩阵，包括旋转、平移、缩放
    float mMVPMatrix[16];//创建用来存放最终变换矩阵的数组
    float mTexCoor[2 * 4];   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
    float mVertxData[3 * 4];
    float mColorBuf[4 * 4];
    GLslHandle mLayerProgram;

    GLint mObjectPositionPointer;
    GLint mVTexCoordPointer;
    GLint mObjectVertColorArrayPointer;
    GLint muMVPMatrixPointer;
    GLuint mFrameBufferPointerArray[2];
    GLuint mRenderBufferPointerArray[2];
    GLuint mFrameBufferTexturePointerArray[2];
    int mFrameCount = 0;
};


#endif //LEARNOPENGL_LAYER_H
