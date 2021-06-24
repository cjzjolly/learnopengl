/* 
@author 陈杰柱
矩阵操作工具类
gcc编译通过*/
#include "matrix.c"

float currMatrix[16];
float mProjMatrix[16];
float mVMatrix[16];
float mMVPMatrix[16];
float mStack[10][16];
int stackTop=-1;

void setInitStack();
void pushMatrix();
void popMatrix();
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


void setInitStack()
{
	setIdentityM(currMatrix,0);
}

void pushMatrix()
{
	stackTop++;
	for(int i=0;i<16;i++)
	{
		mStack[stackTop][i]=currMatrix[i];
	}
}

void popMatrix()
{
	for(int i=0;i<16;i++)
	{
		currMatrix[i]=mStack[stackTop][i];
	}
	stackTop--;
}

void translate(float x,float y,float z)
{
	translateM(currMatrix, 0, x, y, z);
}

void rotate(float angle,float x,float y,float z)
{
	rotateM(currMatrix,0,angle,x,y,z);
}

void scale(float x,float y,float z)
{
	scaleM(currMatrix,0, x, y, z);
}

void setCamera(float cx,float cy,float cz,
 float tx,float ty,float tz,
 float upx,float upy,float upz)
{
	setLookAtM(mVMatrix,0,
    
    cx,cy,cz,

	tx,ty,tz,

	upx,upy,upz);
}

void setProjectFrustum
(
 float left,
 float right,
 float bottom,
 float top,
 float near,
 float far
 )
{
	frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
}

float* getFinalMatrix()
{
	multiplyMM(mMVPMatrix, 0, mVMatrix, 0, currMatrix, 0);
	multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

	return mMVPMatrix;
}
