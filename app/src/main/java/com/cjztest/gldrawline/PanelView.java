package com.cjztest.gldrawline;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class PanelView extends GLSurfaceView {

    public PanelView(Context context) {
        super(context);
        this.setEGLContextClientVersion(3);
        setRenderer(new GLRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
