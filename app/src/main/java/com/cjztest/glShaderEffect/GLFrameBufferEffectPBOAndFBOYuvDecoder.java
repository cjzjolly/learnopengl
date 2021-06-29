package com.cjztest.glShaderEffect;

import android.content.Context;
import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.cjztest.glShaderEffect.ShaderUtil.checkGlError;
import static com.cjztest.glShaderEffect.ShaderUtil.destroyShader;
import static com.cjztest.glShaderEffect.ShaderUtil.loadShader;

/**FBO使用Demo**/
public class GLFrameBufferEffectPBOAndFBOYuvDecoder extends GLLine {
    private final float mX;
    private final float mY;
    private final float mZ;
    private final float mWidth;
    private final float mHeight;
    private final float mWindowW;
    private final float mWindowH;
    private Context mContext;

    FloatBuffer mTexCoorBuffer;//顶点纹理坐标数据缓冲
    //    private int mGenImageTextureId = 0;
    private boolean mIsDestroyed = false;
    private int mAlpha;
    private int mYuvFragShaderPointer;
    private int mYuvVertexShaderPointer;
    private int mYuvBufferDrawProgram;
    private int mObjectPositionPointer;
    private int mGLFrameVTexCoordPointer;
    private int mGLFrameObjectVertColorArrayPointer;
    private int mGLFrameuMVPMatrixPointer;
    private int mFrameCountPointer;
    private int mResoulutionPointer;
    private int mFrameCount = 0;
    private int mFrameBufferWidth;
    private int mFrameBufferHeight;
    private int mGLFrameEffectRPointer;
    private int mGLFrameTargetXYPointer;

    /**是否每次渲染到frameBuffer前都清理**/
    private boolean mFrameBufferClean = false;

    private boolean mFrameBufferCleanOnce = false;

    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGLFrameBufferProgramFunChoicePointer;
    private int mGLFrameObjectPositionPointer;
    private int mImgWidth;
    private int mImgHeight;
    private int[] mFrameBufferPointerArray;
    private int[] mFrameBufferTexturePointerArray;
    private int[] mRenderBufferPointerArray;
    private int mGenYTextureId = 0;
    private int mGenUVTextureId = 0;
    private int mImgPanelYByteSize;
    private int mImgPanelUVByteSize;
    private int mYUVFragShaderPointer;
    private int mYUVVertexShaderPointer;
    private int[] mYPanelPixelBuffferPointerArray;
    private int[] mUVPanelPixelBuffferPointerArray;

    public enum YuvKinds {
        YUV_420SP_UVUV,
        YUV_420SP_VUVU,
        YUV_420P_UUVV,
        YUV_420P_VVUU,
    }
    /**设定YUV类型**/
    private YuvKinds mYuvKinds = YuvKinds.YUV_420SP_UVUV;


    public GLFrameBufferEffectPBOAndFBOYuvDecoder(int baseProgramPointer, float x, float y, float z, float w, float h, int windowW, int windowH, Context context, int imgW, int imgH, YuvKinds yuvKinds) {
        super(baseProgramPointer);
        this.mX = x;
        this.mY = y;
        this.mZ = z;
        this.mWidth = w;
        this.mHeight = h;
        this.mWindowW = windowW;
        this.mWindowH = windowH;
        this.mFrameBufferWidth = windowW;
        this.mFrameBufferHeight = windowH;
        this.mContext = context;
        this.mImgWidth = imgW;
        this.mImgHeight = imgH;
        this.mYuvKinds =  yuvKinds;
        initVertxAndAlpha(0xFF);
        float texCoor[] = new float[]   //纹理内采样坐标,类似于canvas坐标 //这东西有问题，导致两个framebuffer的画面互相取纹理时互为颠倒
                {
                        1f, 0f,
                        0f, 0f,
                        1f, 1f,
                        0f, 1f
                };
        //创建顶点纹理坐标数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(texCoor.length * 4);
        cbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mTexCoorBuffer = cbb.asFloatBuffer();//转换为Float型缓冲
        mTexCoorBuffer.put(texCoor);//向缓冲区中放入顶点纹理数据
        mTexCoorBuffer.position(0);//设置缓冲区起始位置
        startBindTexture();
        createDoubleFrameBuffer();
        createPBO();
    }

    private void initVertxAndAlpha(int alpha) {
        this.mAlpha = alpha;
        int colorJustForAlpha = (0x00FFFFFF | (alpha << 24));
        addPoint(mX + mWidth, mY, mZ, colorJustForAlpha);
        addPoint(mX, mY, mZ, colorJustForAlpha);
        addPoint(mX + mWidth, mY + mHeight, mZ, colorJustForAlpha);
        addPoint(mX, mY + mHeight, mZ, colorJustForAlpha);
    }

    public void setAlpha(int alpha) {
        if (alpha == mAlpha) { //节约资源
            return;
        }
        this.mAlpha = alpha;
        mColorBuf.position(0);
        for (int i = 0; i < 6 * 4; i++) {
            mColorBuf.put(Float.valueOf(String.format("%.2f", (float) alpha / 255f)));
        }
    }

    private void startBindTexture() {
        //特殊纹理，需要专门加载其他程序:
        String fragShaderScript = ShaderUtil.loadFromAssetsFile("yuvconvert/fragShaderYuvConvert.shader", mContext.getResources());
        String vertexShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/vertShader.shader", mContext.getResources());
        //基于顶点着色器与片元着色器创建程序 step_0：编译脚本
        mYuvFragShaderPointer = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderScript);
        mYuvVertexShaderPointer = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderScript);
        mYuvBufferDrawProgram = GLES30.glCreateProgram();
        //若程序创建成功则向程序中加入顶点着色器与片元着色器
        if (mYuvBufferDrawProgram != 0) { //step 1: 创建program后附加编译后的脚本
            //>>>>>>>>>>>>
            //向程序中加入顶点着色器
            GLES30.glAttachShader(mYuvBufferDrawProgram, mYuvFragShaderPointer);
            checkGlError("glAttachShader");
            //向程序中加入片元着色器
            GLES30.glAttachShader(mYuvBufferDrawProgram, mYuvVertexShaderPointer);
            checkGlError("glAttachShader");
            //链接程序
            GLES30.glLinkProgram(mYuvBufferDrawProgram);
            //<<<<<<<<<<<<
            //存放链接成功program数量的数组
            int[] linkStatus = new int[1];
            //获取program的链接情况
            GLES30.glGetProgramiv(mYuvBufferDrawProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
            //若链接失败则报错并删除程序
            if (linkStatus[0] != GLES30.GL_TRUE) { //step 2: 检查是否链接并附加成功，不成功要清理
                Log.e("ES30_ERROR", "Could not link program: ");
                Log.e("ES30_ERROR", GLES30.glGetProgramInfoLog(mYuvBufferDrawProgram));
                GLES30.glDeleteProgram(mYuvBufferDrawProgram);
                mYuvBufferDrawProgram = 0;
            }
        }
        //获取程序中顶点位置属性引用"指针"
        mGLFrameObjectPositionPointer = GLES30.glGetAttribLocation(mYuvBufferDrawProgram, "objectPosition");
        //渲染方式选择
        mGLFrameBufferProgramFunChoicePointer = GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "funChoice");
        //作用半径
        mGLFrameEffectRPointer = GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "effectR");
        //作用位置
        mGLFrameTargetXYPointer = GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "targetXY");
        //纹理采样坐标
        mGLFrameVTexCoordPointer = GLES30.glGetAttribLocation(mYuvBufferDrawProgram, "vTexCoord");
        //获取程序中顶点颜色属性引用"指针"
        mGLFrameObjectVertColorArrayPointer = GLES30.glGetAttribLocation(mYuvBufferDrawProgram, "objectColor");
        //获取程序中总变换矩阵引用"指针"
        mGLFrameuMVPMatrixPointer = GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "uMVPMatrix");
        //渲染帧计数指针
        mFrameCountPointer = GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "frame");
        //设置分辨率指针，告诉gl脚本现在的分辨率
        mResoulutionPointer = GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "resolution");
    }


    /**
     * 创建2个framebuffer作为每次渲染结果的叠加专用纹理
     **/
    private void createDoubleFrameBuffer() {
        int frameBufferCount = 2;

        //生成framebuffer
        mFrameBufferPointerArray = new int[frameBufferCount];
        GLES30.glGenFramebuffers(mFrameBufferPointerArray.length, mFrameBufferPointerArray, 0);

        //生成渲染缓冲buffer
        mRenderBufferPointerArray = new int[frameBufferCount];
        GLES30.glGenRenderbuffers(mRenderBufferPointerArray.length, mRenderBufferPointerArray, 0);

        //生成framebuffer纹理pointer
        mFrameBufferTexturePointerArray = new int[frameBufferCount];
        GLES30.glGenTextures(mFrameBufferTexturePointerArray.length, mFrameBufferTexturePointerArray, 0);

        //遍历framebuffer并初始化
        for (int i = 0; i < frameBufferCount; i++) {
            //绑定帧缓冲，遍历两个framebuffer分别初始化
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferPointerArray[i]);
            //绑定缓冲pointer
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mRenderBufferPointerArray[i]);
            //为渲染缓冲初始化存储，分配显存
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,
                    GLES30.GL_DEPTH_COMPONENT16, mFrameBufferWidth, mFrameBufferHeight); //设置framebuffer的长宽

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mFrameBufferTexturePointerArray[i]); //绑定纹理Pointer

            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置MIN采样方式
                    GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置MAG采样方式
                    GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置S轴拉伸方式
                    GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置T轴拉伸方式
                    GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexImage2D//设置颜色附件纹理图的格式
                    (
                            GLES30.GL_TEXTURE_2D,
                            0,                        //层次
                            GLES30.GL_RGBA,        //内部格式
                            mFrameBufferWidth,            //宽度
                            mFrameBufferHeight,            //高度
                            0,                        //边界宽度
                            GLES30.GL_RGBA,            //格式
                            GLES30.GL_UNSIGNED_BYTE,//每个像素数据格式
                            null
                    );
            GLES30.glFramebufferTexture2D        //设置自定义帧缓冲的颜色缓冲附件
                    (
                            GLES30.GL_FRAMEBUFFER,
                            GLES30.GL_COLOR_ATTACHMENT0,    //颜色缓冲附件
                            GLES30.GL_TEXTURE_2D,
                            mFrameBufferTexturePointerArray[i],                        //纹理id
                            0                                //层次
                    );
            GLES30.glFramebufferRenderbuffer    //设置自定义帧缓冲的深度缓冲附件
                    (
                            GLES30.GL_FRAMEBUFFER,
                            GLES30.GL_DEPTH_ATTACHMENT,        //深度缓冲附件
                            GLES30.GL_RENDERBUFFER,            //渲染缓冲
                            mRenderBufferPointerArray[i]                //渲染深度缓冲id
                    );
        }
        //绑回系统默认framebuffer，否则会显示不出东西
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);//绑定帧缓冲id
    }

    /**
     * 创建2个framebuffer作为每次渲染结果的叠加专用纹理
     **/
    private void createPBO() {
        //创建Y通道PBO
        mImgPanelYByteSize = mImgWidth * mImgHeight;
        mYPanelPixelBuffferPointerArray = new int[2];
        GLES30.glGenBuffers(2, mYPanelPixelBuffferPointerArray, 0);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mYPanelPixelBuffferPointerArray[0]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgPanelYByteSize,  null, GLES30.GL_STREAM_DRAW);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mYPanelPixelBuffferPointerArray[1]);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mImgPanelYByteSize,  null, GLES30.GL_STREAM_DRAW);
        //创建UV通道PBO
        mImgPanelUVByteSize = mImgWidth * mImgHeight / 2;
        mUVPanelPixelBuffferPointerArray = new int[2];
        GLES30.glGenBuffers(2, mUVPanelPixelBuffferPointerArray, 0);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[0]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgPanelUVByteSize,  null, GLES30.GL_STREAM_DRAW);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mUVPanelPixelBuffferPointerArray[1]);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mImgPanelUVByteSize,  null, GLES30.GL_STREAM_DRAW);
    }

    /**绘制画面到framebuffer**/
    private void drawToFrameBuffer(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        GLES30.glUseProgram(mYuvBufferDrawProgram);
        //设置视窗大小及位置
        GLES30.glViewport(0, 0, mFrameBufferWidth, mFrameBufferHeight);
        //绑定帧缓冲id
        if (mFrameCount % 2 == 0) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferPointerArray[0]);
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferPointerArray[1]);
        }
        //清除深度缓冲与颜色缓冲
        if (!mFrameBufferClean && !mFrameBufferCleanOnce) { //实现渲染画面叠加
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            mFrameBufferCleanOnce = true;
        }
        if (mFrameBufferClean) {
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            GLES30.glUniform1i(mGLFrameBufferProgramFunChoicePointer, -1); //第一次加载选择纹理方式渲染
        }
        //设置它的坐标系
        locationTrans(cameraMatrix, projMatrix, this.mGLFrameuMVPMatrixPointer);
        //设置图像分辨率
        GLES30.glUniform2fv(mResoulutionPointer, 1, new float[]{mWindowW, mWindowH}, 0);
        locationTrans(cameraMatrix, projMatrix, this.mGLFrameuMVPMatrixPointer);
        if (mPointBuf != null && mColorBuf != null) {
            if (mFrameCount < 0) {
                mFrameCount = 0;
            }
            mPointBuf.position(0);
            mColorBuf.position(0);
//            GLES30.glUniform1i(GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "sTexture"), 0); //获取纹理属性的指针
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(mGLFrameObjectPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(mGLFrameObjectVertColorArrayPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            //将顶点纹理坐标数据传送进渲染管线
            GLES30.glVertexAttribPointer(mGLFrameVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
            GLES30.glEnableVertexAttribArray(mGLFrameObjectPositionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);  //启用颜色属性
            GLES30.glEnableVertexAttribArray(mGLFrameVTexCoordPointer);  //启用纹理采样定位坐标
//            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            //todo 绘制yuv

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenYTextureId);
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "textureY"), 0); //获取纹理属性的指针

            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenUVTextureId);
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "textureUV"), 1); //获取纹理属性的指针

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(mGLFrameObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameVTexCoordPointer);
        }
        //绑会系统默认framebuffer，否则会显示不出东西
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);//绑定帧缓冲id
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void refreshBuffer(byte[] imgBytes) {
        //更新ypanel数据
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenYTextureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "textureY"), 0); //获取纹理属性的指针
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mYPanelPixelBuffferPointerArray[mFrameCount % 2]);
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, mImgWidth, mImgHeight, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, null); //1字节为一个单位
        //更新图像数据，复制到 PBO 中
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mYPanelPixelBuffferPointerArray[(mFrameCount + 1) % 2]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgPanelYByteSize, null, GLES30.GL_STREAM_DRAW);
        Buffer buf = GLES30.glMapBufferRange(GLES30.GL_PIXEL_UNPACK_BUFFER, 0, mImgPanelYByteSize, GLES30.GL_MAP_WRITE_BIT | GLES30.GL_MAP_INVALIDATE_BUFFER_BIT);
        //填充像素
        ByteBuffer bytebuffer = ((ByteBuffer) buf).order(ByteOrder.nativeOrder());
        bytebuffer.position(0);
        bytebuffer.put(imgBytes, 0, mImgWidth * mImgHeight);
        bytebuffer.position(0);
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);
        //更新uvpanel数据
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenUVTextureId);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "textureUV"), 1); //获取纹理属性的指针
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[mFrameCount % 2]);
        switch (mYuvKinds) {
            default:
            case YUV_420SP_UVUV:
            case YUV_420SP_VUVU:
                GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, mImgWidth / 2, mImgHeight / 2, GLES30.GL_LUMINANCE_ALPHA, GLES30.GL_UNSIGNED_BYTE, null); //2字节为一个单位，所以宽度因为单位为2字节一个，对比1字节时直接对半
                break;
            case YUV_420P_UUVV:
            case YUV_420P_VVUU:
                GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, mImgWidth, mImgHeight / 2, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, null);
                break;
        }
        //更新图像数据，复制到 PBO 中
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[(mFrameCount + 1) % 2]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgPanelUVByteSize, null, GLES30.GL_STREAM_DRAW);
        buf = GLES30.glMapBufferRange(GLES30.GL_PIXEL_UNPACK_BUFFER, 0, mImgPanelUVByteSize, GLES30.GL_MAP_WRITE_BIT | GLES30.GL_MAP_INVALIDATE_BUFFER_BIT);
        //填充像素
        bytebuffer = ((ByteBuffer) buf).order(ByteOrder.nativeOrder());
        bytebuffer.position(0);
        bytebuffer.put(imgBytes, mImgWidth * mImgHeight, mImgWidth * mImgHeight / 2);
        bytebuffer.position(0);
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);
    }


    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        locationTrans(cameraMatrix, projMatrix, mGLFrameuMVPMatrixPointer);
        //先绘制内容到frambuffer
        drawToFrameBuffer(cameraMatrix, projMatrix);
        //绘制
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);//绑定帧系统默认缓冲id
        GLES30.glUseProgram(mYuvBufferDrawProgram);
        locationTrans(cameraMatrix, projMatrix, mGLFrameuMVPMatrixPointer);

        if (mPointBuf != null && mColorBuf != null) {
            Log.i("cjztest", "drawFramebuffer");
            GLES30.glUniform1i(mGLFunChoicePointer, 1); //选择纹理方式渲染
            mPointBuf.position(0);
            mColorBuf.position(0);
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mBaseProgram, "sTexture"), 0); //获取纹理属性的指针
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(mGLFrameObjectVertColorArrayPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            //将顶点纹理坐标数据传送进渲染管线
            GLES30.glVertexAttribPointer(mGLFrameVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
            GLES30.glEnableVertexAttribArray(mObjectPositionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);  //启用颜色属性
            GLES30.glEnableVertexAttribArray(mGLFrameVTexCoordPointer);  //启用纹理采样定位坐标
            //设置面向位置，因为是2d应用，所以不渲染背面，节约资源，参考https://www.jianshu.com/p/ee04165f2a02 >>>
//            GLES30.glEnable(GLES30.GL_CULL_FACE);
//            GLES30.glCullFace(GLES30.GL_FRONT);
            //<<<
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            //切换纹理到当前正在绘制的framebuffer
            if (mFrameCount % 2 == 1) { //todo 如果刷新实时性要求不高，可以只显示一个framebuffer，不然容易因为两者之间存在的微小差异产生震动感
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mFrameBufferTexturePointerArray[0]);
            } else {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mFrameBufferTexturePointerArray[1]);
            }
            GLES30.glUniform1i(mGLFrameBufferProgramFunChoicePointer, -1); //第一次加载选择纹理方式渲染
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mYuvBufferDrawProgram, "textureFBO"), 0); //获取纹理属性的指针
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(mObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameVTexCoordPointer);
//            GLES30.glClearColor(1, 0, 0, 1); //todo cjztest
        }
        GLES30.glUniform1i(mFrameCountPointer, mFrameCount++);

    }

    /**销毁framebuffer**/
    private void destroyFrameBuffer() {
        GLES30.glDeleteTextures(mFrameBufferTexturePointerArray.length, mFrameBufferTexturePointerArray, 0);
        GLES30.glDeleteRenderbuffers(mRenderBufferPointerArray.length, mRenderBufferPointerArray, 0);
        GLES30.glDeleteFramebuffers(mFrameBufferPointerArray.length, mFrameBufferPointerArray, 0);
    }

    public void destroy() {
        if (!mIsDestroyed) {
            destroyFrameBuffer();
            //去除特殊shader程序
            destroyShader(mYuvBufferDrawProgram, mYuvVertexShaderPointer, mYuvFragShaderPointer);
            mContext = null;
        }
        mIsDestroyed = true;
    }

    @Override
    protected void finalize() throws Throwable { //gc时清理已没有应用的纹理，但似乎驱动不喜欢立即释放东西，因此改为复用id的形式
        super.finalize();
        destroy();
    }

    public int getAlpha() {
        return mAlpha;
    }
}
