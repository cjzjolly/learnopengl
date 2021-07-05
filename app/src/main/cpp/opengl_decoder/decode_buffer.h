//
// Created by jiezhuchen on 2021/6/21.
//

#ifndef LEARNOPENGL_DECODE_BUFFER_H
#define LEARNOPENGL_DECODE_BUFFER_H

#include "RenderProgramImage.h"

class DecodeBuffer {
public:
    void drawBuffer(char* data);
private:
    RenderProgramImage *mRenderProgramImage = nullptr;

};


#endif //LEARNOPENGL_DECODE_BUFFER_H
