//
// Created by jiezhuchen on 2021/6/21.
//

#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;
class RenderProgramYUV : public RenderProgram {
public:
    void createRender();

    void drawData(int outputFBOTexturePointer, char *data, int width, int height, int pixelFormat);

    /**todo 如果要绘制的东西本身就是一个纹理呢？*
     * @param outputFBOTexturePointer 最终结果承载FBO，例如图层FBO。各个图层的FBO自底向上渲染*/
    void drawTexture(int outputFBOTexturePointer, int texturePointer, int width, int height);

    int removeTexture(int texturePointerArray[]);

    void destroy();
};
