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
    private Object mLock = new Object();
    /**当前正在执行的渲染器**/
    private GLObject mCurrentRender = null;
    /**模式列表**/
    public enum RENDERER_EFFECT {
        LIGHT_POTS,
        SEA,
        WAVE,

    }
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
    /**当前使用的模式**/
    private RENDERER_EFFECT mCurrentEffect = RENDERER_EFFECT.LIGHT_POTS;

    public void initEffectLayer(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        mBaseProgramPointer = glBaseProgramPointer;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mRenderLight = new GLFragEffectLightPot(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, mWidth, mHeight, mContext);
        mRenderSea = new GLFragEffectSea(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, mWidth, mHeight, mContext);
    }

    /**按图层顺序渲染**/
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        //因为shaderProgram的编译要在GLContext线程中进行，所以挪动创建代码到这个地方
        if (mCurrentRender != null) {
            mCurrentRender.drawTo(cameraMatrix, projMatrix);
            Log.i("cjztest_shader_mode", "渲染线程：" + Thread.currentThread().getName());
        }
        mFrameCount++;
    }

    /**设置当前模式*/
    public void selectMode(RENDERER_EFFECT rendererEffect) {
        mCurrentRender = null;
        switch (rendererEffect) {
            case LIGHT_POTS:
                Log.i("cjztest_shader_mode", "选择了光点：" + Thread.currentThread().getName());
                mCurrentEffect = RENDERER_EFFECT.LIGHT_POTS;
                mCurrentRender = mRenderLight;
                break;
            case SEA:
                mCurrentEffect = RENDERER_EFFECT.SEA;
                mCurrentRender = mRenderSea;
                break;
        }
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
