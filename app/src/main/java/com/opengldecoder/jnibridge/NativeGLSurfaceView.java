package com.opengldecoder.jnibridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.example.learnopengl.R;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeGLSurfaceView extends GLSurfaceView {
    private Bitmap mTestBmp;
    private Renderer mRenderer;
    /**图层native指针**/
    private long mLayer = Long.MIN_VALUE;
    //Android画面数据输入Surface
    private Surface mDataInputSurface = null;
    //Android画面数据输入纹理
    private int[] mDataInputTexturesPointer = null;
    private SurfaceTexture mInputDataSurfaceTexture;
    private int mVideoWidth;
    private int mVideoHeight;

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
//        mTestBmp = BitmapFactory.decodeResource(getResources(), R.drawable.test_pic);
    }

    /**设置OES图层内容得大小**/
    public void setOESLayerSize(int width, int height) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        //添加一个oes渲染器
        JniBridge.addRenderForLayer(mLayer, JniBridge.RENDER_PROGRAM_KIND.RENDER_OES_TEXTURE.ordinal()); //传入oes纹理
    }

    public Surface getSurface() {
        Log.i("cjztest", "GLRenderer.getSurface：" + mDataInputSurface.toString());
        return mDataInputSurface;
    }

    private class Renderer implements GLSurfaceView.Renderer {

        private int mWidth;
        private int mHeight;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if ((width != mWidth || height != mHeight) && width > 0 && height > 0) {
                this.mWidth = width;
                this.mHeight = height;
                Log.i("cjztest", String.format("NativeGlSurfaceView.onSurfaceChanged:width:%d, height:%d", mWidth, mHeight));
                JniBridge.nativeGLInit(width, height);
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
                //清除上次用过的图层
                if (mLayer != Long.MIN_VALUE) {
                    JniBridge.removeLayer(mLayer);
                }
                //创建一个图层（由于这个使用场景种没有数组数据，只有OES纹理，所以dataPointer为0）
                mLayer = JniBridge.addLayer(mDataInputTexturesPointer[0], new int[] {width, height}, 0, new int[] {width, height}, GLES30.GL_RGBA);
                Player mPlayer = new Player(getContext(), getSurface(), new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        setOESLayerSize(width, height);
                    }
                });
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
//            JniBridge.drawBuffer();
//            Log.i("cjztest", "renderining");
            JniBridge.renderLayer(0, mWidth, mHeight);
        }
    }
}
