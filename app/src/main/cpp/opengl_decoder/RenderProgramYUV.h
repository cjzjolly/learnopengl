//
// Created by jiezhuchen on 2021/6/21.
//

#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;
class RenderProgramYUV : public RenderProgram {
public:
    void createRender();

    void drawData(float *cameraMatrix, float *projMatrix, int outputFBOTexturePointer, char *data, int width, int height, int pixelFormat, int offset);

    void destroy();
};
