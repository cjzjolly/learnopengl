//
// Created by jiezhuchen on 2021/6/21.
//

#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;
class RenderProgramImage : public RenderProgram {
public:
    void createRender(float x, float y, float z, float w, float h, int windowW, int windowH);

    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    void loadTexture(int *texturePointers, int width, int height);

    void drawTo(float *cameraMatrix, float *projMatrix, int outputFBOTexturePointer, int fboW, int fboH);

    void destroy();
};
