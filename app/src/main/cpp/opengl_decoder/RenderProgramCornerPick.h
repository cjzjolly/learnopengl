//
// Created by jiezhuchen on 2021/7/7.
//

#ifndef LEARNOPENGL_RENDERPROGRAMCORNERPICK_H
#define LEARNOPENGL_RENDERPROGRAMCORNERPICK_H
#include "shaderUtil.h"
#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;


class RenderProgramCornerPick : public RenderProgram {
public:
    RenderProgramCornerPick();

    ~RenderProgramCornerPick();

    void createRender(float x, float y, float z, float w, float h, int windowW, int windowH);

    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    void loadTexture(GLuint *texturePointers, int width, int height);

    void drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH);

    void destroy();

private:
    /**绑定纹理**/
    GLuint texturePointers[1];
    GLuint mGenTextureId = 0xFFFFFFFF;
    GLslHandle mCornerPickProgram;
    GLint mObjectPositionPointer;
    GLint mVTexCoordPointer;
    GLint mObjectVertColorArrayPointer;
    GLint muMVPMatrixPointer;
    GLint mGLFunChoicePointer;
    GLint mFrameCountPointer;
    GLint mResoulutionPointer;
    GLuint *mInputTextures;
    int mInputTexturesWidth;
    int mInputTexturesHeight;
    int mInputDataWidth;
    int mInputDataHeight;
    int mWindowW, mWindowH;
    bool mIsDestroyed = false;


    char *vertShader;

    char *fragShader;

    float mTexCoor[2 * 4];   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
    float mVertxData[3 * 4];
    float mColorBuf[4 * 4];

    bool mIsTexutresInited = false;
};


#endif //LEARNOPENGL_RENDERPROGRAMCORNERPICK_H
