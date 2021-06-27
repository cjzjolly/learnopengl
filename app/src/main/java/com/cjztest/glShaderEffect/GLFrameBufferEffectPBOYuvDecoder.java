package com.cjztest.glShaderEffect;

import android.content.Context;
import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.RequiresApi;

import static com.cjztest.glShaderEffect.ShaderUtil.checkGlError;
import static com.cjztest.glShaderEffect.ShaderUtil.loadShader;

/**Pixel Buffer PBO Demo**/
public class GLFrameBufferEffectPBOYuvDecoder extends GLLine {
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
    private int mFrameCount = 0;
    private int mImgWidth;
    private int mImgHeight;

    /**是否每次渲染到frameBuffer前都清理**/
    private boolean mFrameBufferClean = false;

    private boolean mFrameBufferCleanOnce = false;

//    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGenImageTextureId = 0;
    private int[] mYPanelPixelBuffferPointerArray;
    private int[] mUVPanelPixelBuffferPointerArray;
    private int[] mUPanelPixelBuffferPointerArray;
    private int[] mVPanelPixelBuffferPointerArray;

    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGenYTextureId = 0;
    private int mGenUVTextureId = 0;
    private int mGenUTextureId = 0;
    private int mGenVTextureId = 0;
    private int mImgPanelYByteSize;
    private int mImgPanelUVByteSize;
    private int mImgPanelUByteSize;
    private int mImgPanelVByteSize;
    private int mYUVFragShaderPointer;
    private int mYUVVertexShaderPointer;
    private int mYUVProgram;
    private int mFrameCountPointer;
    private int mResoulutionPointer;

    public enum YuvKinds {
        YUV_420SP_UVUV,
        YUV_420SP_VUVU,
        YUV_420P_UUVV,
        YUV_420P_VVUU,
    }
    /**设定YUV类型**/
    private YuvKinds mYuvKinds = YuvKinds.YUV_420SP_UVUV;

    public GLFrameBufferEffectPBOYuvDecoder(int baseProgramPointer, float x, float y, float z, float w, float h, int windowW, int windowH, Context context, int imgW, int imgH, YuvKinds yuvKinds) {
        super(baseProgramPointer);
        this.mX = x;
        this.mY = y;
        this.mZ = z;
        this.mWidth = w;
        this.mHeight = h;
        this.mWindowW = windowW;
        this.mWindowH = windowH;
        this.mImgWidth = imgW;
        this.mImgHeight = imgH;
        this.mContext = context;
        this.mYuvKinds = yuvKinds;
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
        startBindEmptyTexture(mYuvKinds);
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

    private void startBindEmptyTexture(YuvKinds yuvKinds) {
        //特殊纹理，需要专门加载其他程序:
        String fragShaderScript = ShaderUtil.loadFromAssetsFile("yuvconvert/fragShaderYuvConvert.shader", mContext.getResources());
        String vertexShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/vertShader.shader", mContext.getResources());
        //基于顶点着色器与片元着色器创建程序 step_0：编译脚本
        mYUVFragShaderPointer = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderScript);
        mYUVVertexShaderPointer = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderScript);
        mYUVProgram = GLES30.glCreateProgram();
        //若程序创建成功则向程序中加入顶点着色器与片元着色器
        if (mYUVProgram != 0) { //step 1: 创建program后附加编译后的脚本
            //>>>>>>>>>>>>
            //向程序中加入顶点着色器
            GLES30.glAttachShader(mYUVProgram, mYUVFragShaderPointer);
            checkGlError("glAttachShader");
            //向程序中加入片元着色器
            GLES30.glAttachShader(mYUVProgram, mYUVVertexShaderPointer);
            checkGlError("glAttachShader");
            //链接程序
            GLES30.glLinkProgram(mYUVProgram);
            //<<<<<<<<<<<<
            //存放链接成功program数量的数组
            int[] linkStatus = new int[1];
            //获取program的链接情况
            GLES30.glGetProgramiv(mYUVProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
            //若链接失败则报错并删除程序
            if (linkStatus[0] != GLES30.GL_TRUE) { //step 2: 检查是否链接并附加成功，不成功要清理
                Log.e("ES30_ERROR", "Could not link program: ");
                Log.e("ES30_ERROR", GLES30.glGetProgramInfoLog(mYUVProgram));
                GLES30.glDeleteProgram(mYUVProgram);
                mYUVProgram = 0;
            }
        }
        //获取程序中顶点位置属性引用"指针"
        mObjectPositionPointer = GLES30.glGetAttribLocation(mYUVProgram, "objectPosition");
        //纹理采样坐标
        mVTexCoordPointer = GLES30.glGetAttribLocation(mYUVProgram, "vTexCoord");
        //获取程序中顶点颜色属性引用"指针"
        mObjectVertColorArrayPointer = GLES30.glGetAttribLocation(mYUVProgram, "objectColor");
        //获取程序中总变换矩阵引用"指针"
        muMVPMatrixPointer = GLES30.glGetUniformLocation(mYUVProgram, "uMVPMatrix");
        //渲染帧计数指针
        mFrameCountPointer = GLES30.glGetUniformLocation(mYUVProgram, "frame");
        //设置分辨率指针，告诉gl脚本现在的分辨率
        mResoulutionPointer = GLES30.glGetUniformLocation(mYUVProgram, "resolution");
        //功能选择
        mGLFunChoicePointer = GLES30.glGetUniformLocation(mBaseProgram, "funChoice");
        //生成textureY纹理
        while(mMapIndexToTextureID.get(mGenYTextureId) != null) {//顺序找到空缺的id
            mGenYTextureId++;
        }
        GLES30.glGenTextures(1, new int[] {mGenYTextureId}, 0); //只要值不重复即可
        //Y纹理初始化
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenYTextureId);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, mImgWidth, mImgHeight, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, null);
        mMapIndexToTextureID.put(mGenYTextureId, 1); //使用该id作为纹理索引指针
        //生成textureUV纹理
        while(mMapIndexToTextureID.get(mGenUVTextureId) != null) {//顺序找到空缺的id
            mGenUVTextureId++;
        }
        GLES30.glGenTextures(1, new int[] {mGenUVTextureId}, 0); //只要值不重复即可
        //UV纹理初始化
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenUVTextureId);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE_ALPHA, mImgWidth, mImgHeight, 0, GLES30.GL_LUMINANCE_ALPHA, GLES30.GL_UNSIGNED_BYTE, null);
        mMapIndexToTextureID.put(mGenUVTextureId, 1); //使用该id作为纹理索引指针
        //生成textureU纹理
        while(mMapIndexToTextureID.get(mGenUTextureId) != null) {//顺序找到空缺的id
            mGenUTextureId++;
        }
        GLES30.glGenTextures(1, new int[] {mGenUTextureId}, 0); //只要值不重复即可
        //U纹理初始化
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenUTextureId);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, mImgWidth, mImgHeight, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, null);
        mMapIndexToTextureID.put(mGenUTextureId, 1); //使用该id作为纹理索引指针
        //生成textureV纹理
        while(mMapIndexToTextureID.get(mGenVTextureId) != null) {//顺序找到空缺的id
            mGenVTextureId++;
        }
        GLES30.glGenTextures(1, new int[] {mGenVTextureId}, 0); //只要值不重复即可
        //V纹理初始化
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenVTextureId);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //创建一个占用指定空间的纹理，但暂时不复制数据进去，等PBO进行数据传输，取代glTexImage2D，利用DMA提高数据拷贝速度
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, mImgWidth, mImgHeight, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, null);
        mMapIndexToTextureID.put(mGenVTextureId, 1); //使用该id作为纹理索引指针
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
        //创建U通道pBO
        mImgPanelUByteSize = mImgWidth * mImgHeight / 2 / 2;
        mUPanelPixelBuffferPointerArray = new int[2];
        GLES30.glGenBuffers(2, mUPanelPixelBuffferPointerArray, 0);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mUPanelPixelBuffferPointerArray[0]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgPanelUByteSize,  null, GLES30.GL_STREAM_DRAW);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mUPanelPixelBuffferPointerArray[1]);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mImgPanelUByteSize,  null, GLES30.GL_STREAM_DRAW);
        //创建V通道PBO
        mImgPanelVByteSize = mImgWidth * mImgHeight / 2 / 2;
        mVPanelPixelBuffferPointerArray = new int[2];
        GLES30.glGenBuffers(2, mVPanelPixelBuffferPointerArray, 0);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mVPanelPixelBuffferPointerArray[0]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgPanelVByteSize,  null, GLES30.GL_STREAM_DRAW);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mVPanelPixelBuffferPointerArray[1]);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mImgPanelVByteSize,  null, GLES30.GL_STREAM_DRAW);
    }

    public void refreshBuffer(byte[] imgBytes) {
//            https://zhuanlan.zhihu.com/p/115257287
//            https://www.jianshu.com/p/1fa36461fc6f?utm_campaign=hugo&utm_medium=reader_share&utm_content=note&utm_source=qq
        //调用 glTexSubImage2D 后立即返回，不影响 CPU 时钟周期
        switch (mYuvKinds) {
            default:
            case YUV_420SP_UVUV:
                //更新ypanel数据
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenYTextureId);
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
                GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mUVPanelPixelBuffferPointerArray[mFrameCount % 2]);
                GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, mImgWidth, mImgHeight / 2 / 2, GLES30.GL_LUMINANCE_ALPHA, GLES30.GL_UNSIGNED_BYTE, null); //2字节为一个单位
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
                break;
            //todo:
            case YUV_420SP_VUVU:
                break;
            case YUV_420P_UUVV:
                break;
            case YUV_420P_VVUU:
                break;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        GLES30.glUseProgram(mYUVProgram);
        switch (mYuvKinds) {
            default:
            case YUV_420SP_UVUV:
                GLES30.glUniform1i(mGLFunChoicePointer, 0);
                break;
            case YUV_420SP_VUVU:
                GLES30.glUniform1i(mGLFunChoicePointer, 1);
                break;
            case YUV_420P_UUVV:
                GLES30.glUniform1i(mGLFunChoicePointer, 2);
                break;
            case YUV_420P_VVUU:
                GLES30.glUniform1i(mGLFunChoicePointer, 3);
                break;
        }
        locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
        if (mPointBuf != null && mColorBuf != null) {
            GLES30.glUniform1i(mGLFunChoicePointer, 1); //选择纹理方式渲染
            mPointBuf.position(0);
            mColorBuf.position(0);
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mBaseProgram, "sTexture"), 0); //获取纹理属性的指针
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectVertColorArrayPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            //将顶点纹理坐标数据传送进渲染管线
            GLES30.glVertexAttribPointer(mVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
            GLES30.glEnableVertexAttribArray(mObjectPositionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(mObjectVertColorArrayPointer);  //启用颜色属性
            GLES30.glEnableVertexAttribArray(mVTexCoordPointer);  //启用纹理采样定位坐标

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(mObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mObjectVertColorArrayPointer);
            GLES30.glDisableVertexAttribArray(mVTexCoordPointer);
        }
//        super.drawTo(programID, positionPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
        mFrameCount++;
    }

    /**销毁PBO**/
    private void destroyPBO() {
        //todo
    }

    public void destroy() {
        if (!mIsDestroyed) {
            destroyPBO();
            //去除特殊shader程序
//            destroyShader(mFrameBufferDrawProgram, mWaveVertexShaderPointer, mWaveFragShaderPointer);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            GLES30.glDeleteTextures(1, new int[] {mGenImageTextureId}, 0); //销毁纹理,gen和delete要成对出现
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
