package com.whiteboard;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.opengldecoder.jnibridge.JniBridge;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WhiteBoardRenderer implements GLSurfaceView.Renderer{
    private int mWidth;
    private int mHeight;
    private boolean mIsFirstFrame = true;

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

    }
}
