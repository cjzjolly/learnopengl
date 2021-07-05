package com.cjztest.glShaderEffect;

import android.opengl.GLES30;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

public abstract class GLObject {
    private final float[] mObjectMatrix = new float[16];    //具体物体的3D变换矩阵，包括旋转、平移、缩放
    protected int mBaseProgram;
    protected float[] mMVPMatrix = new float[16];//创建用来存放最终变换矩阵的数组
    private int mRotatedDegree = 0;
    protected int mObjectPositionPointer;
    protected int mVTexCoordPointer;
    protected int mObjectVertColorArrayPointer;
    protected int muMVPMatrixPointer;
    protected int mGLFunChoicePointer;

    public GLObject() {

    }

    public GLObject(int programPointer) {
        this.mBaseProgram = programPointer;
        //获取程序中顶点位置属性引用"指针"
        mObjectPositionPointer = GLES30.glGetAttribLocation(mBaseProgram, "objectPosition");
        //纹理采样坐标
        mVTexCoordPointer = GLES30.glGetAttribLocation(mBaseProgram, "vTexCoord");
        //获取程序中顶点颜色属性引用"指针"
        mObjectVertColorArrayPointer = GLES30.glGetAttribLocation(mBaseProgram, "objectColor");
        //获取程序中总变换矩阵引用"指针"
        muMVPMatrixPointer = GLES30.glGetUniformLocation(mBaseProgram, "uMVPMatrix");
        //渲染方式选择，0为线条，1为纹理，2为纹理特效，以后还会有光点等等
        mGLFunChoicePointer = GLES30.glGetUniformLocation(mBaseProgram, "funChoice");
        resetObjectMatrix();
    }

    public void resetObjectMatrix() {
        Matrix.setIdentityM(mObjectMatrix, 0);          //初始化变换矩阵，也就是单位矩阵（初始化物体空间, 也可以用来做变换后还原）
    }

    public void scale(float sx, float sy, float sz) {
        Matrix.scaleM(mObjectMatrix, 0, sx, sy, sz);
    }

    public void translate(float dx, float dy, float dz) {
        Matrix.translateM(mObjectMatrix, 0, dx, dy, dz);
    }

    public void rotate(int degree, float roundX, float roundY, float roundZ) {
        mRotatedDegree += degree;
        Matrix.rotateM(mObjectMatrix, 0, degree, roundX, roundY, roundZ);
    }

    public void setRotate(int degree,float roundX, float roundY, float roundZ) { //要用这个方法只能自己保护和还原现场，能不用就不用
        float dx = getDx();
        float dy = getDy();
        float dz = getDz();
        float sx = getSx();
        float sy = getSy();
        float sz = getSz();
        mRotatedDegree = degree;
        Matrix.setRotateM(mObjectMatrix, 0, degree, roundX, roundY, roundZ);
        setDx(dx);
        setDy(dy);
        setDz(dz);
//        setSx(sx);
//        setSy(sy);
//        setSz(sz);
    }

    protected void locationTrans(float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer) { //这里有个坑，opengl的矩阵行转换为列才是熟悉的数学书上矩阵的排布方式，直接按经验乘是错的，要先转换后才符合人类的计算规则
        Matrix.multiplyMM(mMVPMatrix, 0, cameraMatrix, 0, mObjectMatrix, 0);         //将摄像机矩阵乘以物体矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, projMatrix, 0, mMVPMatrix, 0);         //将投影矩阵乘以上一步的结果矩阵
        GLES30.glUniformMatrix4fv(muMVPMatrixPointer, 1, false, mMVPMatrix, 0);        //将最终变换关系传入渲染管线
    }

    public float getDx() {//最后一列在opengl里成了最后一行，所以平移参数要从最后一行里面找，坑，不知道opengl的创造者怎么想的
        return mObjectMatrix[12];
    }

    public float getDy() {
        return mObjectMatrix[13];
    }

    public float getDz() {
        return mObjectMatrix[14];
    }

    public void setDx(float dx) {
        mObjectMatrix[12] = dx;
    }

    public void setDy(float dy) {
        mObjectMatrix[13] = dy;
    }

    public void setDz(float dz) {
        mObjectMatrix[14] = dz;
    }

    public float getSx() {
        return mObjectMatrix[0];
    }

    public float getSy() {
        return mObjectMatrix[5];
    }

    public float getSz() {
        return mObjectMatrix[10];
    }

    public void setSx(float sx) {
        mObjectMatrix[0] = sx;
    }

    public void setSy(float sy) {
        mObjectMatrix[5] = sy;
    }

    public void setSz(float sz) {
        mObjectMatrix[10] = sz;
    }

    public int getRotatedDegree() {
        return mRotatedDegree;
    }

    @NonNull
    @Override
    public String toString() {
        String result = super.toString();
        if (mObjectMatrix != null) {
            result += ":\n";
            result += String.format("[%f, %f, %f, %f]\n", mObjectMatrix[0], mObjectMatrix[4], mObjectMatrix[8], mObjectMatrix[12]);
            result += String.format("[%f, %f, %f, %f]\n", mObjectMatrix[1], mObjectMatrix[5], mObjectMatrix[9], mObjectMatrix[13]);
            result += String.format("[%f, %f, %f, %f]\n", mObjectMatrix[2], mObjectMatrix[6], mObjectMatrix[10], mObjectMatrix[14]);
            result += String.format("[%f, %f, %f, %f]\n", mObjectMatrix[3], mObjectMatrix[7], mObjectMatrix[11], mObjectMatrix[15]);
        }
        return result;
    }

    public abstract void drawTo(float[] cameraMatrix, float[] projMatrix); //安卓的GLES30类中已经有主线程创建的EGL context，直接用就好GLES30绘图就好。
}