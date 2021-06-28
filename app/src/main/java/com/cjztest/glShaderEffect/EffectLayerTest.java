package com.cjztest.glShaderEffect;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.example.learnopengl.R;

import java.util.Arrays;

public class EffectLayerTest implements GLRenderer.onDrawListener {
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
    private GLFrameBufferEffectPBOYuvDecoder mYuvDecoder;
    private int mBaseProgramPointer;
    private byte[] mYuvTestData;
    private byte[] mYuvTestUUVVData;
    private byte[] mYuvTestVVUUData;
    private byte[] mYuvTestData2;
    private int mFrameCount = 0;

    public void initEffectLayer(int glBaseProgramPointer, int windowWidth, int windowHeight, Context context) {
        mWidth = windowWidth;
        mHeight = windowHeight;
        mBaseProgramPointer = glBaseProgramPointer;
        this.mRatio = (float) windowHeight / windowWidth;
        this.mContext = context;
//        mRenderSea = new GLFragEffectSea(mBaseProgramPointer,-1f, -mRatio, 0f, 2, mRatio * 2, windowWidth, windowHeight, context);
//        mRenderLight = new GLFragEffectLightPot(mBaseProgramPointer, -1f, -mRatio, 0f, 2, mRatio * 2, windowWidth, windowHeight, context);
//        mCircle = new GLCircle(mBaseProgramPointer, 1f, 0xFFFF0000, 0xFF0000FF);
//        mFragCircle = new GLFragEffectCircle(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context);
//        mLine = new GLLine(mBaseProgramPointer);
//        mLine.addPoint(-1, -1, 0xFFFF0000);
//        mLine.addPoint(1, 1, 0xFF0000FF);
//        mImage = new GLImage(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic), 1f);
//        mTw = new GLFragEffectTwirlImgEffect(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic));
//        mFBEDC = new GLFrameBufferEffectDrawCircle(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.test_pic_second));
//        mWave = new GLFragEffectWave(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context);
//        mBef1 = new GLFrameBufferEffect1(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context);
//        mPBODemo = new GLFrameBufferEffectPBODemo(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, 100, 100);
        mYuvTestData = ShaderUtil.loadBytesFromAssetsFile("yuvtestdata/degree_90_1024x2048.nv21", context.getResources());
        mYuvTestData2 = ShaderUtil.loadBytesFromAssetsFile("yuvtestdata/degree_270_1024x2048.nv21", context.getResources());
        mYuvTestUUVVData = new byte[mYuvTestData.length];
        mYuvTestVVUUData = new byte[mYuvTestData.length];
        System.arraycopy(mYuvTestData, 0, mYuvTestUUVVData, 0, 1024 * 2048);
        System.arraycopy(mYuvTestData, 0, mYuvTestVVUUData, 0, 1024 * 2048);
        for (int i = 1024 * 2048, j = i; i < mYuvTestUUVVData.length; i += 2, j++) {
            mYuvTestUUVVData[j] = mYuvTestData[i];
        }
        for (int i = 1024 * 2048 + 1, j = 1024 * 2048 + 1024 * 2048 / 2 / 2; i < mYuvTestUUVVData.length; i += 2, j++) {
            mYuvTestUUVVData[j] = mYuvTestData[i];
        }
        for (int i = 1024 * 2048 + 1, j = i - i; i < mYuvTestUUVVData.length; i += 2, j++) {
            mYuvTestVVUUData[j] = mYuvTestData[i];
        }
        for (int i = 1024 * 2048, j = 1024 * 2048 + 1024 * 2048 / 2 / 2; i < mYuvTestUUVVData.length; i += 2, j++) {
            mYuvTestVVUUData[j] = mYuvTestData[i];
        }
//        mYuvDecoder = new GLFrameBufferEffectPBOYuvDecoder(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, 1024, 2048, GLFrameBufferEffectPBOYuvDecoder.YuvKinds.YUV_420P_UUVV);
//        mYuvDecoder = new GLFrameBufferEffectPBOYuvDecoder(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, 1024, 2048, GLFrameBufferEffectPBOYuvDecoder.YuvKinds.YUV_420P_VVUU);
        mYuvDecoder = new GLFrameBufferEffectPBOYuvDecoder(mBaseProgramPointer, -1, -mRatio, 0, 2, mRatio * 2, windowWidth, windowHeight, context, 1024, 2048, GLFrameBufferEffectPBOYuvDecoder.YuvKinds.YUV_420SP_UVUV);
    }

    /**按图层顺序渲染**/
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
//        mRenderSea.drawTo(cameraMatrix, projMatrix);
//        mImage.drawTo(cameraMatrix, projMatrix);
//        mCircle.drawTo(cameraMatrix, projMatrix);
//        mBef1.drawTo(cameraMatrix, projMatrix);
//        mFBEDC.drawTo(cameraMatrix, projMatrix);
//        mTw.drawTo(cameraMatrix, projMatrix);

//        mLine.drawTo(cameraMatrix, projMatrix);
//        mFragCircle.drawTo(cameraMatrix, projMatrix);
//        mWave.drawTo(cameraMatrix, projMatrix);
//        mPBODemo.drawTo(cameraMatrix, projMatrix);
//        byte demoYuv420sp[] = new byte[100 * 100 * 3 / 2];
//        Arrays.fill(demoYuv420sp, (byte) 128);
        long startTime = System.currentTimeMillis();
        mYuvDecoder.refreshBuffer(mFrameCount % 2 == 0 ? mYuvTestData : mYuvTestData2);
//        mYuvDecoder.refreshBuffer(mYuvTestUUVVData);
//        mYuvDecoder.refreshBuffer(mYuvTestVVUUData);
//        mYuvDecoder.refreshBuffer(mYuvTestData);
        mYuvDecoder.drawTo(cameraMatrix, projMatrix);
//        Log.i("cjztest", "refresh and draw cost:" + (System.currentTimeMillis() - startTime) + " ms");
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
