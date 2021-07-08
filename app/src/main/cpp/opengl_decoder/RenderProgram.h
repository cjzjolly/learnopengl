//
// Created by jiezhuchen on 2021/6/21.
//

#ifndef LEARNOPENGL_RENDER_PROGRAM_H
#define LEARNOPENGL_RENDER_PROGRAM_H
#include "matrix.h"
#include "shaderUtil.h"

/**
 * 渲染器基类，通过继承基类实现各种各样的画面渲染器，
 * 实现类可以添加自己独有的函数，例如对比度调节渲染器，
 * 可以添加对比度调节的参数。
 * **/
namespace OPENGL_VIDEO_RENDERER {
    #define GL_SHADER_STRING(SHADER_STR_X)   #SHADER_STR_X

    /**渲染器输入纹理属性表**/
    struct textures{
        GLuint texturePointers;
        int width;
        int height;
    };
    typedef struct textures Textures;

    class RenderProgram {
    public:
        enum DrawType {
            DRAW_DATA,  //绘制已经load好的像素
            DRAW_TEXTURE  //绘制已经load好的纹理
        };

        /**创建对象，创建物体本身作未经处理的坐标（物体空间），编译shader，获取shader属性等
         * @param x,y,z 初始化渲染面左上角（归一式坐标），传到objectMatrix中
         * @param w,h 初始化渲染面（归一式坐标）
         * @param windowW,windowH 渲染面实际分辨率**/
        virtual void createRender(float x, float y, float z, float w, float h, int windowW, int windowH) = 0;

        /**更新显存中对应纹理地址的图像数据
         **/
        virtual void loadData(char *data, int width, int height, int pixelFormat, int offset) = 0;

        /**如果要绘制的东西本身就是纹理，例如YUV渲染器可以输入Y层纹理和UV层纹理两个纹理直接进行渲染
   **/
        virtual void loadTexture(Textures textures[]) = 0;

        /** 把渲染结果绘制到目标frameBufferObject
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染
         * @param fboW,fboH FBO的长和宽**/
        virtual void drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH) = 0;

        /**透明度调节**/
        virtual void setAlpha(float alpha) = 0;

        /**总的资源清理**/
        virtual void destroy() = 0;

        /**把物品顶点变换矩阵初始化为单位矩阵。**/
        void initObjMatrix();

        /**分别沿x,y,z轴放大何种比例**/
        void scale(float sx, float sy, float sz);

        void translate(float dx, float dy, float dz);

        void rotate(int degree,float roundX, float roundY, float roundZ);

        float* getObjectMatrix();

        /**不用translate、scale等方式更改渲染器变换方式，而是选择直接设置**/
        void setObjectMatrix(float objMatrix[]);

        /**传入shaderProgram的最终场景控制指针(ID)，把变换处理后的物品坐标传到muMVPMatrixPointer，
         * 整个场景的控制则由cameraMatrix摄像机矩阵和projMatrix投影矩阵控制
         * @param cameraMatrix 摄像机矩阵，决定你用什么角度和方式看东西
         * @param 决定物体怎么投影到视点
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染**/
        void locationTrans(float cameraMatrix[], float projMatrix[], int muMVPMatrixPointer);

    private:
        float mObjectMatrix[16];    //具体物体的3D变换矩阵，包括旋转、平移、缩放
        float mMVPMatrix[16];//创建用来存放最终变换矩阵的数组
    };
}

#endif
