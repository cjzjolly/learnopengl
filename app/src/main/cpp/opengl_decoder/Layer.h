//
// Created by jiezhuchen on 2021/7/5.
//图层类
//

#ifndef LEARNOPENGL_LAYER_H
#define LEARNOPENGL_LAYER_H
#include "RenderProgram.h"
#include <list>
using namespace OPENGL_VIDEO_RENDERER;


class Layer {
public:
    /** @param x,y,z 初始化图层左上角（归一式坐标），传到objectMatrix中
         * @param w,h 图层的长和宽（归一式坐标）
         * @param windowW,windowH 渲染面实际分辨率**/
    Layer(float x, float y, float z, float w, float h, int windowW, int windowH);

    ~Layer();

    /**渲染程序模板表，可以添加多个渲染模板到表中实现图像流水线式加工**/
    void addRenderProgram(RenderProgram *program);

    /**传入数据源**/
    /**更新显存中对应纹理地址的图像数据
 **/
    void loadData(char *data, int width, int height, int pixelFormat, int offset);

    /**如果要绘制的东西本身就是一个纹理呢**/
    void loadTexture(GLuint *texturePointers, int width, int height);

    /**绘制，遍历mRenderProgramList中的所有渲染模板
     * @param outputFBOPointer 叠加用的FBO，使得图层处理效果和内容可以层层叠加，如果需要fboTexutre本身也可以视为一个处理对象放入drawTexture中进行处理**/
    void drawTo(float *cameraMatrix, float *projMatrix, GLuint outputFBOPointer, int fboW, int fboH);

    void destroy();
private:
    /**渲染程序模板表，可以添加多个渲染模板到表中实现图像流水线式加工**/
    std::list<RenderProgram*> mRenderProgramList;
    float mX;
    float mY;
    float mZ;
    float mWidth;
    float mHeight;
    int mWindowW;
    int mWindowH;
    struct RenderSrcData {
        char* data;
        int width;
        int height;
        int pixelFormat;
        int offset;
    };
    struct RenderSrcData mRenderSrcData;
    struct RenderSrcTexture {
        GLuint *texturePointers;
        int width;
        int height;
    };
    struct RenderSrcTexture mRenderSrcTexture;
};


#endif //LEARNOPENGL_LAYER_H
