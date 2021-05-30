package com.cjztest.coordinateSystem;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.cjztest.gldrawline.GLLine;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by jiezhuchen
 */

public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRenderer";
    private float mRatio = 1f;

    private static float[] mProjMatrix = new float[16];
    private static float[] mVMatrix = new float[16];
    private static float[] mMVPMatrix;
    private int mProgram;
    private int maPositionPointer;
    private int maColorPointer;
    private int muMVPMatrixPointer;
    static float[] mObjectMatrix = new float[16];    //具体物体的3D变换矩阵，包括旋转、平移、缩放
    private GLLine mGLine;


    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        if (shader != 0) { //若创建shader脚本的"指针"成功
            GLES30.glShaderSource(shader, shaderCode);
            GLES30.glCompileShader(shader);
            //存放编译成功shader数量的数组
            int[] compiled = new int[1];
            //获取Shader的编译情况
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {//若编译失败则显示错误日志并删除此shader
                Log.e("ES30_ERROR", "Could not compile shader " + type + ":");
                Log.e("ES30_ERROR", GLES30.glGetShaderInfoLog(shader));
                GLES30.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    //检查每一步操作是否有错误的方法
    public void checkGlError(String op) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e("ES30_ERROR", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    //初始化着色器的initShader方法
    public void initShader() {
        String vertexShaderScript = "#version 300 es\n" +
                "uniform mat4 uMVPMatrix; //总变换矩阵\n" +
                "in vec3 aPosition;  //顶点位置\n" +
                "in vec4 aColor;    //顶点颜色\n" +
                "out vec4 aaColor;  //用于传递给片元着色器的变量\n" +
                "void main()\n" +
                "{\n" +
                "   gl_Position = uMVPMatrix * vec4(aPosition,1); //根据总变换矩阵计算此次绘制此顶点位置\n" +
                "   aaColor = aColor;//将接收的颜色传递给片元着色器\n" +
                "}                      ";
        String fragShaderScript = "#version 300 es\n" +
                "precision mediump float;\n" +
                "in  vec4 aaColor; //接收从顶点着色器过来的参数\n" +
                "out vec4 fragColor;//输出到的片元颜色\n" +
                "void main()\n" +
                "{\n" +
                "   fragColor= aaColor;//给此片元颜色值\n" +
                "}              ";
        //基于顶点着色器与片元着色器创建程序 step_0：编译脚本
        int vertexShaderPointer = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderScript);
        int fragShaderPointer = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderScript);
        mProgram = GLES30.glCreateProgram();
        //若程序创建成功则向程序中加入顶点着色器与片元着色器
        if (mProgram != 0) { //step 1: 创建program后附加编译后的脚本
            //>>>>>>>>>>>>
            //向程序中加入顶点着色器
            GLES30.glAttachShader(mProgram, fragShaderPointer);
            checkGlError("glAttachShader");
            //向程序中加入片元着色器
            GLES30.glAttachShader(mProgram, vertexShaderPointer);
            checkGlError("glAttachShader");
            //链接程序
            GLES30.glLinkProgram(mProgram);
            //<<<<<<<<<<<<
            //存放链接成功program数量的数组
            int[] linkStatus = new int[1];
            //获取program的链接情况
            GLES30.glGetProgramiv(mProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
            //若链接失败则报错并删除程序
            if (linkStatus[0] != GLES30.GL_TRUE) { //step 2: 检查是否链接并附加成功，不成功要清理
                Log.e("ES30_ERROR", "Could not link program: ");
                Log.e("ES30_ERROR", GLES30.glGetProgramInfoLog(mProgram));
                GLES30.glDeleteProgram(mProgram);
                mProgram = 0;
            }
        }
        //获取程序中顶点位置属性引用"指针"
        maPositionPointer = GLES30.glGetAttribLocation(mProgram, "aPosition");
        //获取程序中顶点颜色属性引用"指针"
        maColorPointer = GLES30.glGetAttribLocation(mProgram, "aColor");
        //获取程序中总变换矩阵引用"指针"
        muMVPMatrixPointer = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0f, 0f, 0f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        initShader();
        mGLine = new GLLine();
//        mGLine.addPoint(-1, -1, -1, 0xFFFFFFFF);
//        mGLine.addPoint(0f, 1, 0, 0xFFFF00FF);
//        mGLine.addPoint(1f, -1, -1, 0xFFFF00FF);
        mGLine.addPoint(-1, -1, 0f, 0xFFFFFFFF);
        mGLine.addPoint(-1, 1, 0f, 0xFFFFFFFF);
        mGLine.addPoint(1, 1, 0f, 0xFFFFFFFF);
        mGLine.addPoint(1, -1, 0f, 0xFFFFFFFF);
        mGLine.addPoint(-1, -1, 0f, 0xFFFFFFFF);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: " + width + " " + height);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        this.mRatio = (float) width / height;


        Matrix.frustumM(mProjMatrix, 0, -mRatio * 0.4f, mRatio * 0.4f, -1 * 0.4f, 1 * 0.4f, 1, 50); //视锥体设定
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 6, 0f, 0f, 0f, 0f, 1f, 0.0f); //设定眼球面向方向、看向哪个坐标、眼球上方向（上下方向用于确定倒看还是顺看一个东西）
        Matrix.setRotateM(mObjectMatrix, 0, 0, 0, 1, 0);          //初始化变换矩阵（初始化物体空间, 也可以用来做变换后还原）

        //指定使用某套着色器程序
        GLES30.glUseProgram(mProgram);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);

        //设置沿Z轴正向位移1
//        Matrix.translateM(mObjectMatrix,0,0,0,0f);
        //设置绕y轴旋转yAngle度(每渲染一次都做一次的动画)
        Matrix.rotateM(mObjectMatrix, 0, 1, 0, 1f, 0); //不断累加1度
        //设置绕x轴旋转xAngle度
//        Matrix.rotateM(mObjectMatrix,0,1,1,0,0);
        //将最终变换矩阵传入渲染管线
        mMVPMatrix = new float[16];//创建用来存放最终变换矩阵的数组
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mObjectMatrix, 0);         //将摄像机矩阵乘以变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);         //将投影矩阵乘以上一步的结果矩阵得到最终变换矩阵
        GLES30.glUniformMatrix4fv(muMVPMatrixPointer, 1, false, mMVPMatrix, 0);         //给shader脚本的位置指针送上位置矩阵
        mGLine.drawTo(maPositionPointer, maColorPointer);
        Log.i("cjztest", "drawing line");
    }
}
