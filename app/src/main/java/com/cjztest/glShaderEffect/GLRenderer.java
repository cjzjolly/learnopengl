package com.cjztest.glShaderEffect;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by jiezhuchen
 */

public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRenderer";
    private Context mContext;
    private float mRatio = 1f;

    private static float[] mProjMatrix = new float[16];
    private static float[] mCameraMatrix = new float[16];
    private int mProgram;
    private int mObjectPositionPointer;
    private int mVTexCoordPointer;
    private int mObjectVertColorArrayPointer;
    private int muMVPMatrixPointer;
    private int mGLFrameCountPointer;
    private int mResoulutionPointer;
    private GLLine mGLine;
    /**GL渲染模式选择，指针**/
    private int mGLFunChoicePointer;
    private GLLine mGLine2;
    private GLImage mGLImageDarkWindow;
    private GLImage mGLImage2;
    private int mTest = 0; //cjztest

    public interface onDrawListener {
        void drawTo(int programID, int positionPointer, int vTexCoordPointer, int colorPointer, float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer, int glFunChoicePointer);
        void onSurfaceChanged(int windowWidth, int windowHeight, Context context);
    }
    private onDrawListener mOndrawListener;

    public GLRenderer(Context context) {
        mContext = context;
    }

    public void destroy() {
        mContext = null;
    }

    public void setOndrawListener(onDrawListener ondrawListener) {
        this.mOndrawListener = ondrawListener;
    }

    //检查每一步操作是否有错误的方法
    public static void checkGlError(String op) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e("ES30_ERROR", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

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

    //初始化着色器的initShader方法
    public void initShader() {
        String fragShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/fragShader.shader", mContext.getResources());
        String vertexShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/vertShader.shader", mContext.getResources());
        //基于顶点着色器与片元着色器创建程序 step_0：编译脚本
        int fragShaderPointer = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderScript);
        int vertexShaderPointer = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderScript);
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
        mObjectPositionPointer = GLES30.glGetAttribLocation(mProgram, "objectPosition");
        //纹理采样坐标
        mVTexCoordPointer = GLES30.glGetAttribLocation(mProgram, "vTexCoord");
        //获取程序中顶点颜色属性引用"指针"
        mObjectVertColorArrayPointer = GLES30.glGetAttribLocation(mProgram, "objectColor");
        //获取程序中总变换矩阵引用"指针"
        muMVPMatrixPointer = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        //渲染方式选择，0为线条，1为纹理，2为纹理特效，以后还会有光点等等
        mGLFunChoicePointer = GLES30.glGetUniformLocation(mProgram, "funChoice");
        //时间戳指针
        mGLFrameCountPointer = GLES30.glGetUniformLocation(mProgram, "frame");
        //设置分辨率指针，告诉gl脚本现在的分辨率
        mResoulutionPointer = GLES30.glGetUniformLocation(mProgram, "resolution");
    }

    /**整个场景的平移**/
    public void translate(float dx, float dy, float dz) {
        if (mProjMatrix != null) {
            Matrix.translateM(mProjMatrix, 0, dx, dy, dz);
        }
    }

    /**整个场景的缩放**/
    public void scale(float sx, float sy, float sz) {
        if (mProjMatrix != null) {
            Matrix.scaleM(mProjMatrix, 0, sx, sy, sz);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0f, 0f, 0f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL); //还可以
        //开启透明度混合能力
        GLES30.glEnable( GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);  //todo 这个混合导致光影效果有点问题，需要处理一下
        GLES30.glDisable(GLES30.GL_DITHER);
        initShader();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) { //todo vivizhou:整个画面大小我是按1500*3248这个大小来的哈
        GLES30.glViewport(0, 0, width, height);
        Log.d(TAG, "onSurfaceChanged: " + width + " " + height);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        this.mRatio = (float) height / width;
//        this.mRatio = (float) 3248 / 1500;
        Matrix.frustumM(mProjMatrix, 0, -1, 1, -mRatio, mRatio , 1, 50); //视锥体设定
        Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 1, 0f, 0f, 0f, 0f, 1f, 0.0f); //eyez设定眼球和平面的距离，设定眼球面向方向、看向哪个坐标、眼球上方向（上下方向用于确定倒看还是顺看一个东西）
        //指定使用某套着色器程序
        GLES30.glUseProgram(mProgram);
        if (mOndrawListener != null) {
            mOndrawListener.onSurfaceChanged(width, height, mContext);
        }
        //设置分辨率
        GLES30.glUniform2fv(mResoulutionPointer, 1, new float[] {width, height}, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT); //清理屏幕
        GLES30.glUniform1f(mGLFrameCountPointer, (float) (mTest++ / 100f));
        drawObject();
//        Matrix.rotateM(mProjMatrix, 0, 2f, 0, 0, 1f); //cjztest 按照Z轴旋转
    }

    private void drawObject() {
        if (mOndrawListener != null) {
            mOndrawListener.drawTo(mProgram, mObjectPositionPointer, mVTexCoordPointer, mObjectVertColorArrayPointer, mCameraMatrix, mProjMatrix, muMVPMatrixPointer, mGLFunChoicePointer);
        }
    }
}
