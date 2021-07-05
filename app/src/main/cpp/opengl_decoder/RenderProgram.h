//
// Created by jiezhuchen on 2021/6/21.
//

#ifndef LEARNOPENGL_RENDER_PROGRAM_H
#define LEARNOPENGL_RENDER_PROGRAM_H

namespace OPENGL_VIDEO_RENDERER {
    #define GL_SHADER_STRING(x)   #x

    class RenderProgram {
    public:
        /**创建对象，创建物体本身作未经处理的坐标（物体空间），编译shader，获取shader属性等
         * @param x,y,z 初始化渲染面左上角（归一式坐标）
         * @param w,h 初始化渲染面（归一式坐标）
         * @param windowW,windowH 渲染面实际分辨率**/
        virtual void createRender(float x, float y, float z, float w, float h, int windowW, int windowH) = 0;

        /**更新显存中对应纹理地址的图像数据
         **/
        virtual void loadData(char *data, int width, int height, int pixelFormat, int offset) = 0;

        /**如果要绘制的东西本身就是一个纹理呢**/
        virtual void loadTexture(int *texturePointers, int width, int height) = 0;

        /** 把渲染结果绘制到目标frameBufferObject
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染
         * @param fboW,fboH FBO的长和宽**/
        virtual void drawTo(float *cameraMatrix, float *projMatrix, int outputFBOTexturePointer, int fboW, int fboH) = 0;

        /**总的资源清理**/
        virtual void destroy() = 0;

        /**把物品顶点变换矩阵初始化为单位矩阵。**/
        void initObjMatrix();

        void scale(float sx, float sy, float sz);

        void translate(float dx, float dy, float dz);

        void rotate(int degree,float roundX, float roundY, float roundZ);

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
