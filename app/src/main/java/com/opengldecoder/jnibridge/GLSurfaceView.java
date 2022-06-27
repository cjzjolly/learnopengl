package com.opengldecoder.jnibridge;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.glwhiteboard.WhiteBoardRenderer;

public class GLSurfaceView extends android.opengl.GLSurfaceView {
    private WhiteBoardRenderer mRenderer;

    public GLSurfaceView(Context context) {
        super(context);
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        Log.i("cjztest", "NativeGLSurfaceView222");
    }

    private void init() {
        this.setEGLContextClientVersion(3);//使用OpenGL ES 3.0需设置该参数为3
        mRenderer = new WhiteBoardRenderer();//创建Renderer类的对象
        this.setRenderer(mRenderer);    //设置渲染器
        this.setRenderMode(android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
