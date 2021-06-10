package com.cjztest.glShaderEffect;

import android.content.Context;

public class EffectLayerTest implements GLRenderer.onDrawListener {
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private GLObject mRenderLight;
    private GLFragEffectSea mRenderSea;

    public void initEffectLayer(int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mRenderSea = new GLFragEffectSea(-1f, -mRatio, 0f, 2, mRatio * 2, windowWidth, windowHeight, context);
        mRenderLight = new GLFragEffectLightPot(-1f, -mRatio, 0f, 2, mRatio * 2, windowWidth, windowHeight, context);
    }

    /**按图层顺序渲染**/
    @Override
    public void drawTo(int programID, int positionPointer, int vTexCoordPointer, int colorPointer, float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer, int glFunChoicePointer) {
        mRenderSea.drawTo(programID, positionPointer, vTexCoordPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
        mRenderLight.drawTo(programID, positionPointer, vTexCoordPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
    }

    @Override
    public void onSurfaceChanged(int windowWidth, int windowHeight, Context context) {
        initEffectLayer(windowWidth, windowHeight, context);
    }
}
