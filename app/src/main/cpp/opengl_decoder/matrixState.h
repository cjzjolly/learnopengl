/* 
@author 陈杰柱
矩阵操作工具类
gcc编译通过*/
#ifndef RENDERER_MATRIX_STATE
#define RENDERER_MATRIX_STATE

float currMatrix[16];
float mProjMatrix[16];
float mVMatrix[16];
float mMVPMatrix[16];
float mStack[10][16];
//int stackTop=-1;

//void setInitStack();
//void pushMatrix();
//void popMatrix();
void translate(float x,float y,float z);
void rotate(float angle,float x,float y,float z);
void scale(float x,float y,float z);
void setCamera(float cx,float cy,float cz,
 float tx,float ty,float tz,
 float upx,float upy,float upz);
void setProjectFrustum
(
 float left,
 float right,
 float bottom,
 float top,
 float near,
 float far
 );
float* getFinalMatrix();

#endif

