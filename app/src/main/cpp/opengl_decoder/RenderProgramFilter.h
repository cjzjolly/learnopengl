//
// Created by jiezhuchen on 2021/6/21.
//

#include "shaderUtil.h"
#include "RenderProgram.h"

using namespace OPENGL_VIDEO_RENDERER;
class RenderProgramFilter : public RenderProgram {
public:
    RenderProgramFilter();

    ~RenderProgramFilter();

    void createRender(float x, float y, float z, float w, float h, int windowW, int windowH);

    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    void setAlpha(float alpha);

    void setBrightness(float brightness);

    void setContrast(float contrast);

    void setWhiteBalance(float redWeight, float greenWeight, float blueWeight);

    void loadTexture(Textures textures[]);

    void loadLut(char *lutPixels, int lutWidth, int lutHeight, int unitLength);

    void drawTo(float *cameraMatrix, float *projMatrix, DrawType drawType, int outputFBOPointer, int fboW, int fboH);

    void destroy();

private:
    /**绑定纹理**/
    GLuint mTexturePointers[1];
    GLuint mLutTexutresPointers[1];

    GLuint mGenTextureId = 0xFFFFFFFF;
    GLuint mInputTexturesArrayPointer;
    GLslHandle mImageProgram;
    GLint mObjectPositionPointer;
    GLint mVTexCoordPointer;
    GLint mObjectVertColorArrayPointer;
    GLint muMVPMatrixPointer;
    GLint mGLFunChoicePointer;
    GLint mFrameCountPointer;
    GLint mResoulutionPointer;
    int mWindowW, mWindowH;
    bool mIsDestroyed = false;

    char *vertShader;

    char *fragShader;

    float mTexCoor[2 * 4];   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
    float mVertxData[3 * 4];
    float mColorBuf[4 * 4];

    int mDataWidth;
    int mDataHeight;
    int mInputTextureWidth;
    int mInputTextureHeight;

    bool mIsTexutresInited = false;

    char* mLutPixels = nullptr;
    int mLutWidth;
    int mLutHeight;
    int mLutUnitLen;
    bool mHadLoadLut = false;

};
