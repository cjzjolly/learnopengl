//
// Created by jiezhuchen on 2021/6/21.
//

#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;
class RenderProgramYUV : public RenderProgram {
public:
    void createRender();

    void requestRender(int outputFBOTexturePointer, int texturesPointer[], int dataArrayLength);
};
