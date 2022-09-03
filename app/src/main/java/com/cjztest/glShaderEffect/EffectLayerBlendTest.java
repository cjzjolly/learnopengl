package com.cjztest.glShaderEffect;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.example.learnopengl.R;

import java.util.Arrays;

public class EffectLayerBlendTest implements GLRenderer.onDrawListener {
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private GLObject mRenderLight;
    private GLFragEffectSea mRenderSea;
    private GLFragEffectCircle mFragCircle;
    private GLCircle mCircle;
    private GLLine mLine;
    private GLImage mImage;
    private GLFragEffectTwirlImgEffect mTw;
    private GLFrameBufferEffectDrawCircle mFBEDC;
    private GLFragEffectWave mWave;
    private GLFrameBufferEffect1 mBef1;
    private GLFrameBufferEffectPBODemo mPBODemo;
    private int mBaseProgramPointer;
    private int mFrameCount = 0;

    public void initEffectLayer(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        mBaseProgramPointer = glBaseProgramPointer;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mImage = new GLImage(mBaseProgramPointer, -1, -mRatio, 0, 2 / 2f, mRatio * 2 / 2f, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic), 1f);
        mRenderLight = new GLFragEffectLightPot(mBaseProgramPointer, -1, -mRatio, 0,2, mRatio * 2, windowWidth, windowHeight, context);
        mFBEDC = new GLFrameBufferEffectDrawCircle(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic_second));
    }

    /**按图层顺序渲染**/
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        mImage.drawTo(cameraMatrix, projMatrix);
        mRenderLight.drawTo(cameraMatrix, projMatrix);
        mFrameCount++;
    }

    @Override
    public void onSurfaceChanged(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        initEffectLayer(glBaseProgramPointer, windowWidth, windowHeight, context);
    }

    @Override
    public void onTouch(MotionEvent event) {
//        mFBEDC.onTouch(event);
    }
}
