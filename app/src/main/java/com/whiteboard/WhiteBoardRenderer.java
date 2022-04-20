package com.whiteboard;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WhiteBoardRenderer implements GLSurfaceView.Renderer{
    private int mWidth;
    private int mHeight;
    private boolean mIsFirstFrame = true;
    private MotionEvent mTouchEvent = null;
    private WhiteBoardGLSurfaceView mGLSurfaceView = null;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if ((width != mWidth || height != mHeight) && width > 0 && height > 0) {
            this.mWidth = width;
            this.mHeight = height;
            Log.i("cjztest", String.format("NativeGlSurfaceView.onSurfaceChanged:width:%d, height:%d", mWidth, mHeight));
            JniBridge.nativeGLInit(width, height);
            mIsFirstFrame = true;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mTouchEvent != null) {
            JniBridge.touchAndDraw(mTouchEvent.getX(), mTouchEvent.getY(), mTouchEvent.getAction());
        }
    }

    public void setTouchEvent(MotionEvent touchEvent) {
        this.mTouchEvent = touchEvent;
        if (mGLSurfaceView != null) {
            //先获取触摸信息，再触发渲染onDrawFrame //todo 最好加一些锁处理:
            mGLSurfaceView.requestRender();
        }
    }

    public void setGLSurfaceView(WhiteBoardGLSurfaceView glSurfaceView) {
        this.mGLSurfaceView = glSurfaceView;
    }
}
