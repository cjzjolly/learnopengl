//
// Created by jiezhuchen on 2021/7/5.
//

#ifndef LEARNOPENGL_OPENGLNATIVELIB_H
#define LEARNOPENGL_OPENGLNATIVELIB_H
#include "matrix.h"
#include "Layer.h"

class OpenGLNativeLib {
public:
    void setupGraphics(int w, int h, float *bgColor);
    void draw();
private:
    Layer *mLayer;
    float mProjMatrix[16];
    float mCameraMatrix[16];
    float mRatio;
    int mWidth;
    int mHeight;
};


#endif //LEARNOPENGL_OPENGLNATIVELIB_H
