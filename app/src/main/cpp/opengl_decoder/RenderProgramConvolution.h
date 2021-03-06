//
// Created by jiezhuchen on 2021/7/7.
//

#ifndef LEARNOPENGL_RENDERPROGRAMCONVOLUTION_H
#define LEARNOPENGL_RENDERPROGRAMCONVOLUTION_H
#include "shaderUtil.h"
#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;


class RenderProgramConvolution : public RenderProgram {
public:
    RenderProgramConvolution(float conKernel[]);

    ~RenderProgramConvolution();

    void createRender(float x, float y, float z, float w, float h, int windowW, int windowH);

    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    void setAlpha(float alpha);

    void setBrightness(float brightness);

    void setContrast(float contrast);

    void setWhiteBalance(float redWeight, float greenWeight, float blueWeight);

    void loadTexture(Textures textures[]);

    void drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH);

    void destroy();

    /**更新卷积核
     * @param kernel 卷积核**/
    void refreshConvolutionKernel(float kernel[]);

private:
    /**绑定纹理**/
    GLuint mTexturePointers[1];
    GLuint mGenTextureId = 0xFFFFFFFF;
    GLslHandle mCornerPickProgram;
    GLint mObjectPositionPointer;
    GLint mVTexCoordPointer;
    GLint mObjectVertColorArrayPointer;
    GLint muMVPMatrixPointer;
    GLint mGLFunChoicePointer;
    GLint mFrameCountPointer;
    GLint mResoulutionPointer;
    GLint mConvolutionKernel;
    GLuint mInputTextures;
    int mInputTexturesWidth;
    int mInputTexturesHeight;
    int mInputDataWidth;
    int mInputDataHeight;
    int mWindowW, mWindowH;
    bool mIsDestroyed = false;


    char *vertShader;

    char *fragShader;

    float mConKernel[9];
    float mTexCoor[2 * 4];   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
    float mVertxData[3 * 4];
    float mColorBuf[4 * 4];

    bool mIsTexutresInited = false;
};


#endif //LEARNOPENGL_RENDERPROGRAMCONVOLUTION_H
