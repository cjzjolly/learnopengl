package com.cjztest.glShaderEffect;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class PanelView extends GLSurfaceView {

    private GLRenderer mRender;

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PanelView(Context context) {
        super(context);
        init();
    }

    private void init() {
        this.setEGLContextClientVersion(3);
        mRender = new GLRenderer(getContext());
        setRenderer(mRender);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public GLRenderer getRender() {
        return mRender;
    }
}
