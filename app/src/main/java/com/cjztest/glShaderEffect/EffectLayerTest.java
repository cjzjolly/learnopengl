package com.cjztest.glShaderEffect;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.example.learnopengl.R;

public class EffectLayerTest implements GLRenderer.onDrawListener {
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private GLObject mRenderLight;
    private GLFragEffectSea mRenderSea;
    private GLCircle mCircle;
    private GLLine mLine;
    private GLImage mImage;
    private int mBaseProgramPointer;

    public void initEffectLayer(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        mBaseProgramPointer = glBaseProgramPointer;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mRenderSea = new GLFragEffectSea(mBaseProgramPointer,-1f, -mRatio, 0f, 2, mRatio * 2, windowWidth, windowHeight, context);
        mRenderLight = new GLFragEffectLightPot(mBaseProgramPointer, -1f, -mRatio, 0f, 2, mRatio * 2, windowWidth, windowHeight, context);
        mCircle = new GLCircle(mBaseProgramPointer, 1f, 0xFFFF0000, 0xFF0000FF);
        mLine = new GLLine(mBaseProgramPointer);
        mLine.addPoint(-1, -1, 0xFFFF0000);
        mLine.addPoint(1, 1, 0xFF0000FF);
        mImage = new GLImage(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic), 1f);
    }

    /**按图层顺序渲染**/
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
//        mRenderSea.drawTo(cameraMatrix, projMatrix);
        mImage.drawTo(cameraMatrix, projMatrix);
        mCircle.drawTo(cameraMatrix, projMatrix);
        mRenderLight.drawTo(cameraMatrix, projMatrix);
        mLine.drawTo(cameraMatrix, projMatrix);
    }

    @Override
    public void onSurfaceChanged(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        initEffectLayer(glBaseProgramPointer, windowWidth, windowHeight, context);
    }
}
