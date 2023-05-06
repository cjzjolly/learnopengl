package com.cjztest.gldrawlinesByMultiVectors;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class LinesCanvasSurface extends GLSurfaceView {
    private final SceneRenderer mRenderer;
    private int mWidth;
    private int mHeight;
    /**线条列表**/
    private List<GLLine> mLines = new ArrayList<>();
    private GLLine mCurrentLine;
    private int mProgram;
    private int maPositionPointer;
    private int maColorPointer;
    private int muMVPMatrixPointer;


    public LinesCanvasSurface(Context context) {
        super(context);
        this.setEGLContextClientVersion(3); //设置使用OPENGL ES3.0
        setEGLConfigChooser(new MSAAConfigChooser());
        mRenderer = new SceneRenderer();	//创建场景渲染器
        setRenderer(mRenderer);				//设置渲染器
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//设置渲染模式为主动渲染
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

    //检查每一步操作是否有错误的方法
    public void checkGlError(String op) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e("ES30_ERROR", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public class MSAAConfigChooser implements GLSurfaceView.EGLConfigChooser {
        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

            int attribs[] = {
                    EGL10.EGL_LEVEL, 0,
                    EGL10.EGL_RENDERABLE_TYPE, 4,  // EGL_OPENGL_ES2_BIT
                    EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 24,
                    EGL10.EGL_SAMPLE_BUFFERS, 1,
                    EGL10.EGL_SAMPLES, 4,  // 在这里修改MSAA的倍数，4就是4xMSAA，再往上开程序可能会崩
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] configCounts = new int[1];
            egl.eglChooseConfig(display, attribs, configs, 1, configCounts);

            if (configCounts[0] == 0) {
                // Failed! Error handling.
                return null;
            } else {
                return configs[0];
            }
        }
    }

    //初始化着色器的initShader方法
    public void initShader() {
        String vertexShaderScript = "#version 300 es\n" +
                "uniform mat4 uMVPMatrix; //总变换矩阵（场景的缩放、旋转、平移）\n" +
                "in vec3 aPosition;  //物体顶点位置\n" +
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
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentLine = new GLLine();
                break;
            case MotionEvent.ACTION_MOVE:
                if (null == mCurrentLine) {
                    break;
                }
                if (mWidth <= 0 || mHeight <= 0) {
                    break;
                }
                mCurrentLine.addPoint((e.getX() / mWidth - 0.5f) * 3f * Constant.ratio,  (0.5f - e.getY() / mHeight) * 3f, Color.GREEN);
                break;
            case MotionEvent.ACTION_UP:
                mLines.add(mCurrentLine);
                break;
        }
        requestRender();
        return true;
    }

    private class SceneRenderer implements Renderer {


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            //设置屏幕背景色RGBA
            GLES30.glClearColor(0.0f,0.0f,0.0f,1.0f);
            //打开深度检测
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glDepthFunc(GLES30.GL_LEQUAL); //还可以
            //混合模式等
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
            GLES30.glDisable(GLES30.GL_DITHER);
            //打开背面剪裁(demo 不要开)
//            GLES30.glEnable(GLES30.GL_CULL_FACE);
            //初始化变换矩阵
            MatrixState.setInitStack();
            initShader();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mWidth = width;
            mHeight = height;
            //设置视窗大小及位置
            GLES30.glViewport(0, 0, width, height);
            //计算GLSurfaceView的宽高比
            Constant.ratio = (float) width / height;
            // 调用此方法计算产生透视投影矩阵
            MatrixState.setProjectFrustum(-Constant.ratio, Constant.ratio, -1, 1, 20, 100);
            // 调用此方法产生摄像机9参数位置矩阵
            MatrixState.setCamera(0, 0f, 30, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            //清除深度缓冲与颜色缓冲
            GLES30.glClear( GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            //设置屏幕背景色RGBA
            GLES30.glClearColor(0f,0f,0f, 1.0f);
            GLES30.glUseProgram(mProgram);
            GLES30.glUniformMatrix4fv(muMVPMatrixPointer, 1, false, MatrixState.getFinalMatrix(), 0);         //给shader脚本的位置指针送上位置矩阵


            //遍历所有线条并绘制
            for (GLLine line : mLines) {
                if (null == line) {
                    continue;
                }
                FloatBuffer lineVerts = line.getPointBuf();
                FloatBuffer colors = line.getColorBuf();
                if (lineVerts == null || colors == null) {
                    continue;
                }
                lineVerts.position(0);
                colors.position(0);
                //将顶点位置数据送入渲染管线
                GLES30.glVertexAttribPointer
                        (
                                maPositionPointer,
                                3,
                                GLES30.GL_FLOAT,
                                false,
                                0,  //stride是啥？
                                lineVerts
                        );
                //将顶点颜色数据送入渲染管线
                GLES30.glVertexAttribPointer
                        (
                                maColorPointer,
                                4,
                                GLES30.GL_FLOAT,
                                false,
                                0,
                                colors
                        );
                GLES30.glEnableVertexAttribArray(maPositionPointer); //启用顶点属性
                GLES30.glEnableVertexAttribArray(maColorPointer);  //启用颜色属性
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, line.getPointBufferPos() / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            }

            if (null != mCurrentLine) {
                FloatBuffer lineVerts = mCurrentLine.getPointBuf();
                FloatBuffer colors = mCurrentLine.getColorBuf();
                if (lineVerts != null && colors != null) {
                    lineVerts.position(0);
                    colors.position(0);
                    //将顶点位置数据送入渲染管线
                    GLES30.glVertexAttribPointer
                            (
                                    maPositionPointer,
                                    3,
                                    GLES30.GL_FLOAT,
                                    false,
                                    0,  //stride是啥？
                                    lineVerts
                            );
                    //将顶点颜色数据送入渲染管线
                    GLES30.glVertexAttribPointer
                            (
                                    maColorPointer,
                                    4,
                                    GLES30.GL_FLOAT,
                                    false,
                                    0,
                                    colors
                            );
                    GLES30.glEnableVertexAttribArray(maPositionPointer); //启用顶点属性
                    GLES30.glEnableVertexAttribArray(maColorPointer);  //启用颜色属性
//                    GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, mCurrentLine.getPointBufferPos() / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mCurrentLine.getPointBufferPos() / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
                }
            }
        }
    }



}
