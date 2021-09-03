package com.opengldecoder.jnibridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeGLSurfaceView extends GLSurfaceView {
    private Bitmap mTestBmp;
    private Renderer mRenderer;
    /**图层native指针**/
    private long mLayer = Long.MIN_VALUE;
    private long mRenderOES = Long.MIN_VALUE;
    private long mRenderConvolutionDemo = Long.MIN_VALUE;
    //Android画面数据输入Surface
    private Surface mDataInputSurface = null;
    //Android画面数据输入纹理
    private int[] mDataInputTexturesPointer = null;
    private SurfaceTexture mInputDataSurfaceTexture;
    private Player mDemoPlayer;


    public NativeGLSurfaceView(Context context) {
        super(context);
    }

    public NativeGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        Log.i("cjztest", "NativeGLSurfaceView222");
    }

    private void init() {
        this.setEGLContextClientVersion(3);//使用OpenGL ES 3.0需设置该参数为3
        mRenderer = new Renderer();//创建Renderer类的对象
        this.setRenderer(mRenderer);    //设置渲染器
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public Surface getSurface() {
        Log.i("cjztest", "GLRenderer.getSurface：" + mDataInputSurface.toString());
        return mDataInputSurface;
    }

    /**亮度调整**/
    public void setRenderBrightness (float brightness) {
        if (mRenderOES != Long.MIN_VALUE) {
            JniBridge.setBrightness(mRenderOES, brightness);
        }
    }

    /**白平衡调整**/
    public void setRenderWhiteBalance (float rWeight, float gWeight, float bWeight) {
        if (mRenderOES != Long.MIN_VALUE) {
            JniBridge.setWhiteBalance(mRenderOES, rWeight, gWeight, bWeight);
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {

        private int mWidth;
        private int mHeight;
        private int mVideoWidth;
        private int mVideoHeight;
        private boolean mIsFirstFrame = true;


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.i("cjztest", String.format("NativeGlSurfaceView.onSurfaceCreated"));
            mWidth = 0;
            mHeight = 0;
            mVideoWidth = 0;
            mVideoHeight = 0;
            mIsFirstFrame = true;
            //创建一个OES纹理和相关配套对象
            if (mDataInputSurface == null) {
                //创建OES纹理
                mDataInputTexturesPointer = new int[1];
                GLES30.glGenTextures(1, mDataInputTexturesPointer, 0);
                GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mDataInputTexturesPointer[0]);
                //设置放大缩小。设置边缘测量
                GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
                mInputDataSurfaceTexture = new SurfaceTexture(mDataInputTexturesPointer[0]);
                mDataInputSurface = new Surface(mInputDataSurfaceTexture);
            }
            //创建一个demo播放器
            if (mDemoPlayer == null) {
                mDemoPlayer = new Player(getContext(), getSurface(), new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        /**设置OES图层内容得大小**/
                        if ((width != mVideoWidth || height != mVideoHeight) && width > 0 && height > 0) {
                            Log.i("cjztest", String.format("onSurfaceChanged: w:%d, h:%d", width, height));
                            mVideoWidth = width;
                            mVideoHeight = height;
                        }
                    }
                });
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if ((width != mWidth || height != mHeight) && width > 0 && height > 0) {
                this.mWidth = width;
                this.mHeight = height;
                Log.i("cjztest", String.format("NativeGlSurfaceView.onSurfaceChanged:width:%d, height:%d", mWidth, mHeight));
                JniBridge.nativeGLInit(width, height);
                mIsFirstFrame = true;
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mIsFirstFrame) {  //不能异步进行gl操作，所以只能移到第一帧（或glrender的各种回调中，但这里需要等待onVideoSizeChanged准备好）进行图层创建
                if (mVideoWidth > 0 && mVideoHeight > 0) {
                    //清除上次用过的图层
                    if (mLayer != Long.MIN_VALUE) {
                        JniBridge.removeLayer(mLayer);
                    }
                    //创建一个图层（由于这个使用场景种没有数组数据，只有OES纹理，所以dataPointer为0）
                    mLayer = JniBridge.addFullContainerLayer(mDataInputTexturesPointer[0], new int[]{mVideoWidth, mVideoHeight}, 0, new int[]{0, 0}, GLES30.GL_RGBA);  //依次传入纹理、纹理的宽高、数据地址（如果有）、数据的宽高
                    //添加一个oes渲染器
                    mRenderOES = JniBridge.addRenderForLayer(mLayer, JniBridge.RENDER_PROGRAM_KIND.RENDER_OES_TEXTURE.ordinal()); //添加oes纹理
//                    mRenderConvolutionDemo = JniBridge.addRenderForLayer(mLayer, JniBridge.RENDER_PROGRAM_KIND.RENDER_CONVOLUTION.ordinal()); //添加卷积图像处理demo
                    mIsFirstFrame = false;
                }
            }
            mInputDataSurfaceTexture.updateTexImage();
            JniBridge.renderLayer(0, mWidth, mHeight);
        }
    }
}
