package com.cjz.littleps;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;

import com.cjztest.glShaderEffect.GLCircle;
import com.cjztest.glShaderEffect.GLFragEffectCircle;
import com.cjztest.glShaderEffect.GLFragEffectSea;
import com.cjztest.glShaderEffect.GLFragEffectTwirlImgEffect;
import com.cjztest.glShaderEffect.GLFragEffectWave;
import com.cjztest.glShaderEffect.GLFrameBufferEffect1;
import com.cjztest.glShaderEffect.GLFrameBufferEffectPingPongSave;
import com.cjztest.glShaderEffect.GLImage;
import com.cjztest.glShaderEffect.GLLine;
import com.cjztest.glShaderEffect.GLObject;
import com.cjztest.glShaderEffect.GLRenderer;
import com.example.learnopengl.R;

public class PSEffectLayer implements GLRenderer.onDrawListener {
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private GLFrameBufferEffectPingPongSave mFBEDC;
    private int mBaseProgramPointer;

    public void initEffectLayer(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        mBaseProgramPointer = glBaseProgramPointer;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mFBEDC = new GLFrameBufferEffectPingPongSave(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic_second));
    }

    /**
     * 按图层顺序渲染
     **/
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        mFBEDC.drawTo(cameraMatrix, projMatrix);
    }

    @Override
    public void onSurfaceChanged(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        initEffectLayer(glBaseProgramPointer, windowWidth, windowHeight, context);
    }

    @Override
    public void onTouch(MotionEvent event) {
        mFBEDC.onTouch(event);
    }

    /**PS功能选择**/
    public void setPSFunciton(GLFrameBufferEffectPingPongSave.PSFunciton funciton) {
        mFBEDC.setPSFunciton(funciton);
    }
}
