package com.cjztest.gldrawlinesByMultiVectors;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.cjztest.glMyLightModelSimple.Constant;
import com.cjztest.glMyLightModelSimple.LightDot;
import com.cjztest.glMyLightModelSimple.MatrixState;
import com.cjztest.glMyLightModelSimple.RoomBox;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LinesCanvasSurface extends GLSurfaceView {
    private final SceneRenderer mRenderer;
    private int mWidth;
    private int mHeight;
    /**线条列表**/
    private List<GLLine> mLines = new ArrayList<>();
    private GLLine mCurrentLine;


    public LinesCanvasSurface(Context context) {
        super(context);
        this.setEGLContextClientVersion(3); //设置使用OPENGL ES3.0
        mRenderer = new SceneRenderer();	//创建场景渲染器
        setRenderer(mRenderer);				//设置渲染器
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//设置渲染模式为主动渲染
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
                mCurrentLine.addPoint(e.getX() / mWidth, e.getY() / mHeight, Color.WHITE);
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



            //todo 遍历所有线条并绘制
            for (GLLine line : mLines) {
                FloatBuffer lineVerts = line.getPointBuf();
            }
        }
    }



}
