package com.cjztest.glmediaplayer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;

public class TextureViewMediaActivity extends Activity {
    private static final String TAG = "GLViewMediaActivity";
    private GLSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //强制横屏：
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(new GLVideoRendererFadeInFadeOut(this));//设置renderer

        setContentView(glView);

    }

}