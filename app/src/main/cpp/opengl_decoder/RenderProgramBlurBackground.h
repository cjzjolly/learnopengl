//
// Created by Administrator on 2022/9/14.
//

//#ifndef LEARNOPENGL_RENDERPROGRAMBLURBACKGROUND_H
//#define LEARNOPENGL_RENDERPROGRAMBLURBACKGROUND_H
//
//#endif //LEARNOPENGL_RENDERPROGRAMBLURBACKGROUND_H

#include "shaderUtil.h"
#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;
class RenderProgramBlurBackground : public RenderProgram {
public:
    RenderProgramBlurBackground();

    ~RenderProgramBlurBackground();

    void createRender(float x, float y, float z, float w, float h, int windowW, int windowH);

    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    void setAlpha(float alpha);

    void setBrightness(float brightness);

    void setContrast(float contrast);

    void setWhiteBalance(float redWeight, float greenWeight, float blueWeight);

    void loadTexture(Textures textures[]);

    void drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH);

    void destroy();

private:
    int mWindowW, mWindowH;
    float mTexCoor[2 * 4];   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
    float mVertxData[3 * 4];
    float mColorBuf[4 * 4];
    GLslHandle mImageProgram;
    GLint mObjectPositionPointer;
    GLint mVTexCoordPointer;
    GLint mObjectVertColorArrayPointer;
    GLint muMVPMatrixPointer;

    char *vertShader;

    char *fragShader;
    GLuint mInputTexturesArray;
    int mInputTextureWidth;
    int mInputTextureHeight;
};
