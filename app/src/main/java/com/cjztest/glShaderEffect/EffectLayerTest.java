package com.cjztest.glShaderEffect;

import android.content.Context;

public class EffectLayerTest implements GLRenderer.onDrawListener {
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private GLObject mRenderLight;
    private GLCircle mGLCircie;

    public void initEffectLayer(int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mGLCircie = new GLCircle(1f, 0xFFFF0000, 0xFF0000FF);
        mRenderLight = new GLFragEffectLightPot(-1f, -mRatio, 0f, 2, mRatio * 2, context);
    }

    /**按图层顺序渲染**/
    @Override
    public void drawTo(int programID, int positionPointer, int vTexCoordPointer, int colorPointer, float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer, int glFunChoicePointer) {
        mRenderLight.drawTo(programID, positionPointer, vTexCoordPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
        mGLCircie.drawTo(programID, positionPointer, vTexCoordPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
    }

    @Override
    public void onSurfaceChanged(int windowWidth, int windowHeight, Context context) {
        initEffectLayer(windowWidth, windowHeight, context);
    }
}
