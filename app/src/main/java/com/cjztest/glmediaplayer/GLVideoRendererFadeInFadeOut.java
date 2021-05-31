package com.cjztest.glmediaplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2018/8/16 0016.
 */

public class GLVideoRendererFadeInFadeOut implements GLSurfaceView.Renderer
        , SurfaceTexture.OnFrameAvailableListener, MediaPlayer.OnVideoSizeChangedListener {


    private static final String TAG = "GLRenderer";
    private Context context;
    private int aPositionLocation;
    private int programId;
    private FloatBuffer vertexBuffer;
    private final float[] vertexData = {
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
    };

    private final float[] projectionMatrix = new float[16];
    private int uMatrixLocation;

    private final float[] textureVertexData = {
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
    };
    private float mAlpha = 1f;
    private FloatBuffer textureVertexBuffer;
    private int uTextureSamplerLocation;
    private int aTextureCoordLocation;
    private int textureId;

    private SurfaceTexture surfaceTexture;
    private MediaPlayer mediaPlayer;
    private float[] mSTMatrix = new float[16];
    private int uSTMMatrixHandle;

    private boolean updateSurface;
    private int screenWidth, screenHeight;
    private int mColorAlphaPointer;
    private float mDelta = 0.01f; //每帧透明度变换

    public GLVideoRendererFadeInFadeOut(Context context) {
        this.context = context;
        synchronized (this) {
            updateSurface = false;
        }
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);
        initMediaPlayer();
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexShader = "attribute vec4 aPosition;\n" +  //顶点位置
                "attribute vec4 aTexCoord;\n" +  //S T 纹理坐标
                "attribute float alpha;\n" +  //颜色差异偏移长度
                "varying vec2 vTexCoord;\n" +
                "varying float colorAlpha;\n" +
                "uniform mat4 uMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "void main() {\n" +
                "    vTexCoord = (uSTMatrix * aTexCoord).xy;\n" +
                "    gl_Position = uMatrix*aPosition;\n" +
                "    colorAlpha = alpha;\n" +
                "}";
        String fragmentShaderDouYin =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "varying float colorAlpha;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "\n" +
                        "        vec4 rgba = texture2D(sTexture, vTexCoord);\n" +
                        "        vec4 rgbFixed = rgba * colorAlpha;\n" +
                        "        rgbFixed[3] = 1.0;\n" +
                        "        gl_FragColor = rgbFixed;\n" + //向量组（1维矩阵）里面得每个值都乘以透明度权重
                        "\n" +
                        "}";
        //加载shader脚本 <<<
        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, loadShader(GLES20.GL_VERTEX_SHADER, vertexShader));
        GLES20.glAttachShader(programId, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderDouYin));
        GLES20.glLinkProgram(programId);
        GLES20.glUseProgram(programId);
        //加载shader脚本 >>>

        aPositionLocation = GLES20.glGetAttribLocation(programId, "aPosition");

        uMatrixLocation = GLES20.glGetUniformLocation(programId, "uMatrix");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");
        uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord");
        mColorAlphaPointer = GLES20.glGetAttribLocation(programId, "alpha");


        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
//        ShaderUtils.checkGlError("glBindTexture mTextureID");
   /*GLES11Ext.GL_TEXTURE_EXTERNAL_OES的用处？
      之前提到视频解码的输出格式是YUV的（YUV420p，应该是），那么这个扩展纹理的作用就是实现YUV格式到RGB的自动转化，
      我们就不需要再为此写YUV转RGB的代码了*/
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);//监听是否有新的一帧数据到来

        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);

    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("car_race.mp4");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
//            String path = "http://192.168.1.254:8192";
//            mediaPlayer.setDataSource(path);
//            mediaPlayer.setDataSource(TextureViewMediaActivity.videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnVideoSizeChangedListener(this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: " + width + " " + height);
        screenWidth = width;
        screenHeight = height;
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        synchronized (this) {
            if (updateSurface) {
                surfaceTexture.updateTexImage();//获取新数据
                surfaceTexture.getTransformMatrix(mSTMatrix);//让新的纹理和纹理坐标系能够正确的对应,mSTMatrix的定义是和projectionMatrix完全一样的。
                updateSurface = false;
            }
        }
        GLES20.glUseProgram(programId);
        //试试加个旋转：
//        Matrix.rotateM(projectionMatrix,0,2,1f,1f,1); //不断累加1度
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer);

        textureVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);
        //启用颜色偏移并赋值：
//        FloatBuffer colorOffsetXYBuffer = ByteBuffer.allocateDirect(mColorLayerOffsetXY.length * 4)
//                .order(ByteOrder.nativeOrder())
//                .asFloatBuffer()
//                .put(mColorLayerOffsetXY);
//        colorOffsetXYBuffer.position(0);
//        GLES20.glEnableVertexAttribArray(mColorOffsetShaderPointer);
//        GLES20.glVertexAttribPointer(mColorOffsetShaderPointer, 1, GLES20.GL_FLOAT, false, 2, colorOffsetXYBuffer);
        GLES20.glVertexAttrib1f(mColorAlphaPointer, mAlpha);
           //透明度：
        if (mAlpha >= 1f || mAlpha <= 0f) {
            mDelta = -mDelta;
        }
        mAlpha += mDelta;
        //
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glUniform1i(uTextureSamplerLocation, 0);
        GLES20.glViewport(0, 0, screenWidth, screenHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.d(TAG, "onVideoSizeChanged: " + width + " " + height);
        updateProjection(width, height);
    }

    private void updateProjection(int videoWidth, int videoHeight) {
        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
