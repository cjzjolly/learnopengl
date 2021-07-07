//
// Created by jiezhuchen on 2021/6/21.
//

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>
#include "RenderProgram.h"
#include "matrix.c"
#include "shaderUtil.c"

using namespace OPENGL_VIDEO_RENDERER;

void RenderProgram::initObjMatrix() {
    //创建单位矩阵
    setIdentityM(mObjectMatrix, 0);
}

void RenderProgram::scale(float sx, float sy, float sz) {
    scaleM(mObjectMatrix, 0, sx, sy, sz);
}

void RenderProgram::translate(float dx, float dy, float dz) {
    translateM(mObjectMatrix, 0, dx, dy, dz);
}

void RenderProgram::rotate(int degree, float roundX, float roundY, float roundZ) {
    rotateM(mObjectMatrix, 0, degree, roundX, roundY, roundZ);
}

void RenderProgram::locationTrans(float cameraMatrix[], float projMatrix[], int muMVPMatrixPointer) {
    multiplyMM(mMVPMatrix, 0, cameraMatrix, 0, mObjectMatrix, 0);         //将摄像机矩阵乘以物体矩阵
    multiplyMM(mMVPMatrix, 0, projMatrix, 0, mMVPMatrix, 0);         //将投影矩阵乘以上一步的结果矩阵
    glUniformMatrix4fv(muMVPMatrixPointer, 1, false, mMVPMatrix);        //将最终变换关系传入渲染管线
}
