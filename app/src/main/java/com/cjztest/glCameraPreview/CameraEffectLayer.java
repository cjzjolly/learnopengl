package com.cjztest.glCameraPreview;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.cjztest.glShaderEffect.GLCircle;
import com.cjztest.glShaderEffect.GLFragEffectCircle;
import com.cjztest.glShaderEffect.GLFragEffectSea;
import com.cjztest.glShaderEffect.GLFragEffectTwirlImgEffect;
import com.cjztest.glShaderEffect.GLFragEffectWave;
import com.cjztest.glShaderEffect.GLFrameBufferEffect1;
import com.cjztest.glShaderEffect.GLFrameBufferEffectDrawCircle;
import com.cjztest.glShaderEffect.GLFrameBufferEffectPBODemo;
import com.cjztest.glShaderEffect.GLFrameBufferEffectPBOYuvDecoder;
import com.cjztest.glShaderEffect.GLImage;
import com.cjztest.glShaderEffect.GLLine;
import com.cjztest.glShaderEffect.GLObject;
import com.cjztest.glShaderEffect.GLRenderer;
import com.cjztest.glShaderEffect.ShaderUtil;

public class CameraEffectLayer implements GLRenderer.onDrawListener {
    private int mCameraWidth;
    private int mCameraHeight;
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private GLFrameBufferEffectPBOYuvDecoder mYuvDecoder;
    private int mBaseProgramPointer;
    private int mFrameCount = 0;
    private byte[] mYuv;

    public CameraEffectLayer(int width, int height) {
        this.mCameraWidth = width;
        this.mCameraHeight = height;
    }

    public void initEffectLayer(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        mBaseProgramPointer = glBaseProgramPointer;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
        mYuvDecoder = new GLFrameBufferEffectPBOYuvDecoder(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, mWidth, mHeight, mContext, mCameraWidth, mCameraHeight, GLFrameBufferEffectPBOYuvDecoder.YuvKinds.YUV_420SP_VUVU);
    }

    /**按图层顺序渲染**/
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        long startTime = System.currentTimeMillis();
        if (mYuvDecoder != null) {
            if (mYuv != null) {
                mYuvDecoder.refreshBuffer(mYuv);
            }
            mYuvDecoder.drawTo(cameraMatrix, projMatrix);
        }
        mFrameCount++;
    }

    @Override
    public void onSurfaceChanged(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        initEffectLayer(glBaseProgramPointer, windowWidth, windowHeight, context);
    }

    @Override
    public void onTouch(MotionEvent event) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void refreshYUV(byte yuv[]) {
        mYuv = yuv;
    }
}
