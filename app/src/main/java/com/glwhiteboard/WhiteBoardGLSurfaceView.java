package com.glwhiteboard;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class WhiteBoardGLSurfaceView extends GLSurfaceView {
    private int mCurrentAction;
    private float mEventX;
    private float mEventY;
    private WhiteBoardRenderer mRenderer;

    public WhiteBoardGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public WhiteBoardGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.setEGLContextClientVersion(3);
        mRenderer = new WhiteBoardRenderer();
        mRenderer.setGLSurfaceView(this);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCurrentAction = event.getAction();
        //传入renderer里:
        if (mRenderer != null) {
            mRenderer.setTouchEvent(event);
        }
        return true;
    }
}
