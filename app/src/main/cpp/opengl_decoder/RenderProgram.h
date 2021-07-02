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
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染
         **/
        virtual void drawData(int outputFBOTexturePointer, char *data, int width, int height, int pixelFormat) = 0;

        /**todo 如果要绘制的东西本身就是一个纹理呢？*
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染*/
        virtual void drawTexture(int outputFBOTexturePointer, int texturePointer, int width, int height) = 0;

        /**总的资源清理**/
        virtual void destroy() = 0;

        /**申请渲染
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染**/
        virtual void requestRender(int outputFBOTexturePointer) = 0;

    private:

    };
}

#endif
