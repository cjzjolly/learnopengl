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

        /**申请渲染
         * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染
         * @param texturesPointer 传入已通过texImage2D或PBO放入数据的textures，渲染时映射到shader uniform sample2d对象上**/
        virtual void requestRender(int outputFBOTexturePointer, int texturesPointer[], int dataArrayLength) = 0;


    private:

    };
}

#endif
