package com.cjztest.glmediaplayer;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;

public class TextureViewMediaActivity extends Activity {
    private static final String TAG = "GLViewMediaActivity";
    private GLSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        GLVideoRenderer glVideoRenderer = new GLVideoRenderer(this);//创建renderer
        glView.setRenderer(glVideoRenderer);//设置renderer

        setContentView(glView);

    }

}