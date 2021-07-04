//
// Created by jiezhuchen on 2021/6/21.
//

#ifndef LEARNOPENGL_RENDER_PROGRAM_H
#define LEARNOPENGL_RENDER_PROGRAM_H

namespace OPENGL_VIDEO_RENDERER {
    #define GL_SHADER_STRING(x)   #x

    class RenderProgram {
    public:
        /**创建对象，编译shader，获取shader属性等**/
        virtual void createRender() = 0;

        /**更新显存中对应纹理地址的图像数据
         * @param cameraMatrix 摄像机矩阵，决定你用什么角度和方式看东西
         * @param 决定物体怎么投影到视点
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染
         **/
        virtual void drawData(float *cameraMatrix, float *projMatrix, int outputFBOTexturePointer, char *data, int width, int height, int pixelFormat, int offset) = 0;

        /**todo 如果要绘制的东西本身就是一个纹理呢？*
         * @param cameraMatrix 摄像机矩阵，决定你用什么角度和方式看东西
         * @param 决定物体怎么投影到视点
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染*/
        virtual void drawTexture(float *cameraMatrix, float *projMatrix, int outputFBOTexturePointer, int texturePointers[], int width, int height) = 0;

        /**总的资源清理**/
        virtual void destroy() = 0;

    private:

    };
}

#endif
