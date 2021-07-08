//
// Created by jiezhuchen on 2021/7/5.
//

#ifndef LEARNOPENGL_OPENGLNATIVERENDER_H
#define LEARNOPENGL_OPENGLNATIVERENDER_H
#include "matrix.h"
#include "Layer.h"

class OpenGLNativeRender {
public:
    void setupGraphics(int w, int h, float *bgColor);
    void drawRGBA(char *buf, int w, int h);
private:
    Layer *mLayer;
    Layer *mLayer2;
    float mProjMatrix[16];
    float mCameraMatrix[16];
    float mRatio;
    int mWidth;
    int mHeight;
};


#endif //LEARNOPENGL_OPENGLNATIVERENDER_H
