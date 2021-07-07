//
// Created by jiezhuchen on 2021/7/5.
//

#include <GLES3/gl3.h>
#include "Layer.h"
#include "RenderProgram.h"
using namespace OPENGL_VIDEO_RENDERER;


Layer::Layer(float x, float y, float z, float w, float h, int windowW, int windowH) {
    mX = x;
    mY = y;
    mZ = z;
    mWidth = w;
    mHeight = h;
    mWindowW = windowW;
    mWindowH = windowH;
    mRenderSrcData.data = nullptr;
    mRenderSrcTexture.texturePointers = nullptr;
}

Layer::~Layer() {
    destroy();
}

void Layer::destroy() {

}

void Layer::addRenderProgram(RenderProgram *program) {
    mRenderProgramList.push_back(program);
}

/**给每个模板传入渲染数据**/
void Layer::loadData(char *data, int width, int height, int pixelFormat, int offset) {
    mRenderSrcData.data = data;
    mRenderSrcData.width = width;
    mRenderSrcData.height = height;
    mRenderSrcData.pixelFormat = pixelFormat;
    mRenderSrcData.offset = offset;
}

/**@param texturePointers 可以用于渲染已经绑定好的纹理，或者直接传入FBO，把上一个图层的结果进一步进行渲染，例如叠加图片、或者进行毛玻璃效果处理。当然也可以在一个图层上叠加更多渲染器实现，但多图层便于不同画面不同大小的重叠，渲染器在同一个图层中大小保持一致**/
void Layer::loadTexture(GLuint *texturePointers, int width, int height) {
    mRenderSrcTexture.texturePointers = texturePointers;
    mRenderSrcTexture.width = width;
    mRenderSrcTexture.height = height;
}

/**逐步加工绘制**/
void Layer::drawTo(float *cameraMatrix, float *projMatrix, GLuint outputFBOPointer, int fboW, int fboH) {
    int i = 0;
    for (auto item = mRenderProgramList.begin(); item != mRenderProgramList.end(); item++, i++) {
        //第一个渲染器接受图层原始数据，其他的从上一个渲染结果中作为输入
        if (i == 0) {
            if (mRenderSrcData.data != nullptr) {
                (*item)->loadData(mRenderSrcData.data, mRenderSrcData.width, mRenderSrcData.height, mRenderSrcData.pixelFormat, mRenderSrcData.offset);
            }
            if (mRenderSrcTexture.texturePointers != nullptr) {
                (*item)->loadTexture(mRenderSrcTexture.texturePointers, mRenderSrcTexture.width, mRenderSrcTexture.height);
            }
        } else { //如果只有一个渲染器则走不到else里，否则第0个打后的渲染器依次使用上一个渲染器的结果，也就是图层FBO中的数据作为输入
            GLuint textures[] = {outputFBOPointer};
            (*item)->loadTexture(textures, fboW, fboH); //使用上一个渲染器的渲染结果作为绘制输入
        }
        (*item)->drawTo(cameraMatrix, projMatrix, RenderProgram::DRAW_DATA, outputFBOPointer, fboW, fboH);
    }
}


