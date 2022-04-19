//
// Created by jiezhuchen on 2021/6/21.
//

#ifndef RENDERER_OPENGL_MATRIX
#define RENDERER_OPENGL_MATRIX

void multiplyMM(float *result, int resultOffset, float *mlIn, int lhsOffset, float *mrIn, int rhsOffset);
void multiplyMV(float *resultVec, int resultVecOffset, float *mlIn, int lhsMatOffset,
                float *vrIn, int rhsVecOffset);
void setIdentityM(float *sm, int smOffset);
void translateM(float *m, int mOffset, float x, float y, float z);
void rotateM(float *m, int mOffset, float a, float x, float y, float z);
void setRotateM(float *m, int mOffset, float a, float x, float y, float z);
void scaleM(float *m, int mOffset, float x, float y, float z);
void transposeM(float *mTrans, int mTransOffset, float *m, int mOffset);
void frustumM(float *m, int offset, float left, float right, float bottom, float top, float near, float far);
void setLookAtM(float *rm, int rmOffset, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ);

#endif //RENDERER_OPENGL_MATRIX
