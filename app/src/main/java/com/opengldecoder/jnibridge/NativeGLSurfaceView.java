package com.opengldecoder.jnibridge;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeGLSurfaceView extends GLSurfaceView {
    private Renderer mRenderer;

    public NativeGLSurfaceView(Context context) {
        super(context);
        this.setEGLContextClientVersion(3);//使用OpenGL ES 3.0需设置该参数为3
        mRenderer = new Renderer();//创建Renderer类的对象
        this.setRenderer(mRenderer);    //设置渲染器
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public NativeGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private class Renderer implements GLSurfaceView.Renderer {

        private int mWidth;
        private int mHeight;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (width != mWidth || height != mHeight) {
                JniBridge.nativeGLInit(width, height);
            }
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }
    }
}
