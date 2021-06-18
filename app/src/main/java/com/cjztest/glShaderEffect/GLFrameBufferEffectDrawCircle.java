package com.cjztest.glShaderEffect;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.cjztest.glShaderEffect.ShaderUtil.checkGlError;
import static com.cjztest.glShaderEffect.ShaderUtil.destroyShader;
import static com.cjztest.glShaderEffect.ShaderUtil.loadShader;

public class GLFrameBufferEffectDrawCircle extends GLLine {
    private final float mX;
    private final float mY;
    private final float mZ;
    private final float mWidth;
    private final float mHeight;
    private final float mWindowW;
    private final float mWindowH;
    private Context mContext;

    FloatBuffer mTexCoorBuffer;//顶点纹理坐标数据缓冲
//    private int mGenTextureId = 0;
    private boolean mIsDestroyed = false;
    private int mAlpha;
    private int mWaveFragShaderPointer;
    private int mWaveVertexShaderPointer;
    private int mFrameBufferDrawProgram;
    private int mObjectPositionPointer;
    private int mGLFrameVTexCoordPointer;
    private int mGLFrameObjectVertColorArrayPointer;
    private int mGLFrameuMVPMatrixPointer;
    private int mFrameCountPointer;
    private int mResoulutionPointer;
    private int mFrameCount = 0;
    private int mFrameBufferPointer;
    private int mRenderDepthBufferPointer;
    private int mFrameBufferWidth;
    private int mFrameBufferHeight;
    private int mFrameBufferTexturePointer;
    private int mGLFrameEffectRPointer;
    private int mGLFrameTargetXYPointer;

    /**是否每次渲染到frameBuffer前都清理**/
    private boolean mFrameBufferClean = false;

    private boolean mFrameBufferCleanOnce = false;

    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGenTextureId = 0;
    private int mGLFrameBufferProgramFunChoicePointer;
    private int mGLFrameObjectPositionPointer;
    private float mEventX;
    private float mEventY;


    public GLFrameBufferEffectDrawCircle(int baseProgramPointer, float x, float y, float z, float w, float h, int windowW, int windowH, Context context, Bitmap bitmap) {
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
        initVertxAndAlpha(0xFF);
        float texCoor[] = new float[]   //纹理内采样坐标,类似于canvas坐标
                {
                        0, 1,
                        0, 0,
                        1, 0,

                        1, 0,
                        1, 1,
                        0, 1,
                };
        //创建顶点纹理坐标数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(texCoor.length * 4);
        cbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mTexCoorBuffer = cbb.asFloatBuffer();//转换为Float型缓冲
        mTexCoorBuffer.put(texCoor);//向缓冲区中放入顶点纹理数据
        mTexCoorBuffer.position(0);//设置缓冲区起始位置
        startBindTexture(bitmap);
        createFrameBuffer();
        rotate(0, 0, 0, 1);
    }

    private void initVertxAndAlpha(int alpha) {
        this.mAlpha = alpha;
        int colorJustForAlpha = (0x00FFFFFF | (alpha << 24));
        addPoint(mX, mY, mZ, colorJustForAlpha);
        addPoint(mX, mY + mHeight, mZ, colorJustForAlpha);
        addPoint(mX + mWidth, mY + mHeight, mZ, colorJustForAlpha);
        addPoint(mX + mWidth, mY + mHeight, mZ, colorJustForAlpha);
        addPoint(mX + mWidth, mY, mZ, colorJustForAlpha);
        addPoint(mX, mY, mZ, colorJustForAlpha);
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

    private void startBindTexture(Bitmap bitmap) {
        //特殊纹理，需要专门加载其他程序:
        String fragShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/fragShaderFrameBufferEffectPicProccesser.shader", mContext.getResources());
        String vertexShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/vertShader.shader", mContext.getResources());
        //基于顶点着色器与片元着色器创建程序 step_0：编译脚本
        mWaveFragShaderPointer = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderScript);
        mWaveVertexShaderPointer = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderScript);
        mFrameBufferDrawProgram = GLES30.glCreateProgram();
        //若程序创建成功则向程序中加入顶点着色器与片元着色器
        if (mFrameBufferDrawProgram != 0) { //step 1: 创建program后附加编译后的脚本
            //>>>>>>>>>>>>
            //向程序中加入顶点着色器
            GLES30.glAttachShader(mFrameBufferDrawProgram, mWaveFragShaderPointer);
            checkGlError("glAttachShader");
            //向程序中加入片元着色器
            GLES30.glAttachShader(mFrameBufferDrawProgram, mWaveVertexShaderPointer);
            checkGlError("glAttachShader");
            //链接程序
            GLES30.glLinkProgram(mFrameBufferDrawProgram);
            //<<<<<<<<<<<<
            //存放链接成功program数量的数组
            int[] linkStatus = new int[1];
            //获取program的链接情况
            GLES30.glGetProgramiv(mFrameBufferDrawProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
            //若链接失败则报错并删除程序
            if (linkStatus[0] != GLES30.GL_TRUE) { //step 2: 检查是否链接并附加成功，不成功要清理
                Log.e("ES30_ERROR", "Could not link program: ");
                Log.e("ES30_ERROR", GLES30.glGetProgramInfoLog(mFrameBufferDrawProgram));
                GLES30.glDeleteProgram(mFrameBufferDrawProgram);
                mFrameBufferDrawProgram = 0;
            }
        }
        //获取程序中顶点位置属性引用"指针"
        mGLFrameObjectPositionPointer = GLES30.glGetAttribLocation(mFrameBufferDrawProgram, "objectPosition");
        //渲染方式选择
        mGLFrameBufferProgramFunChoicePointer = GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "funChoice");
        //作用半径
        mGLFrameEffectRPointer = GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "effectR");
        //作用位置
        mGLFrameTargetXYPointer = GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "targetXY");
        //纹理采样坐标
        mGLFrameVTexCoordPointer = GLES30.glGetAttribLocation(mFrameBufferDrawProgram, "vTexCoord");
        //获取程序中顶点颜色属性引用"指针"
        mGLFrameObjectVertColorArrayPointer = GLES30.glGetAttribLocation(mFrameBufferDrawProgram, "objectColor");
        //获取程序中总变换矩阵引用"指针"
        mGLFrameuMVPMatrixPointer = GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "uMVPMatrix");
        //渲染帧计数指针
        mFrameCountPointer = GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "frame");
        //设置分辨率指针，告诉gl脚本现在的分辨率
        mResoulutionPointer = GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "resolution");
        //送纹理进显存
        while(mMapIndexToTextureID.get(mGenTextureId) != null) {//顺序找到空缺的id
            mGenTextureId++;
        }
        GLES30.glGenTextures(1, new int[] {mGenTextureId}, 0); //只要值不重复即可
        //绑定处理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenTextureId);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, mBmpW, mBmpW, 0, GL_RGBA, GL_UNSIGNED_BYTE, 4);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle(); //图片已送入显存，所以可以从内存中释放了
        mMapIndexToTextureID.put(mGenTextureId, 1); //使用该id作为纹理索引指针
    }


    /**
     * 创建一个framebuffer作为每次渲染结果的叠加专用纹理
     **/
    private void createFrameBuffer() {
        int bufferPointerArray[] = new int[1];
        GLES30.glGenFramebuffers(1, bufferPointerArray, 0);
        //取出已赋值的frameBuffer指针
        mFrameBufferPointer = bufferPointerArray[0];
        //绑定帧缓冲:
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferPointer);

        //渲染缓冲buffer
        int renderBufferPointerArray[] = new int[1];
        GLES30.glGenRenderbuffers(1, renderBufferPointerArray, 0);
        mRenderDepthBufferPointer = renderBufferPointerArray[0];
        //绑定缓冲pointer
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mRenderDepthBufferPointer);
        //为渲染缓冲初始化存储，分配显存
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,
                GLES30.GL_DEPTH_COMPONENT16, mFrameBufferWidth, mFrameBufferHeight); //设置framebuffer的长宽

        //
        int texturePointerArray[] = new int[1];
        GLES30.glGenTextures(1, texturePointerArray, 0);
        mFrameBufferTexturePointer = texturePointerArray[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mFrameBufferTexturePointer); //绑定纹理Pointer

        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置MIN采样方式
                GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置MAG采样方式
                GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置S轴拉伸方式
                GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,//设置T轴拉伸方式
                GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D//设置颜色附件纹理图的格式
                (
                        GLES30.GL_TEXTURE_2D,
                        0,						//层次
                        GLES30.GL_RGBA, 		//内部格式
                        mFrameBufferWidth,			//宽度
                        mFrameBufferHeight,			//高度
                        0,						//边界宽度
                        GLES30.GL_RGBA,			//格式
                        GLES30.GL_UNSIGNED_BYTE,//每个像素数据格式
                        null
                );
        GLES30.glFramebufferTexture2D		//设置自定义帧缓冲的颜色缓冲附件
                (
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_COLOR_ATTACHMENT0,	//颜色缓冲附件
                        GLES30.GL_TEXTURE_2D,
                        mFrameBufferTexturePointer, 						//纹理id
                        0								//层次
                );
        GLES30.glFramebufferRenderbuffer	//设置自定义帧缓冲的深度缓冲附件
                (
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_DEPTH_ATTACHMENT,		//深度缓冲附件
                        GLES30.GL_RENDERBUFFER,			//渲染缓冲
                        mRenderDepthBufferPointer				//渲染深度缓冲id
                );
        //绑会系统默认framebuffer，否则会显示不出东西
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);//绑定帧缓冲id
    }


    private void destroyFrameBuffer() {
        GLES30.glDeleteTextures(1, new int[] {mFrameBufferTexturePointer}, 0);
        GLES30.glDeleteRenderbuffers(1, new int[] {mRenderDepthBufferPointer}, 0);
        GLES30.glDeleteFramebuffers(1, new int[] {mFrameBufferPointer}, 0);
    }

    /**绘制画面到framebuffer**/
    private void drawToFrameBuffer(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        GLES30.glUseProgram(mFrameBufferDrawProgram);
        //设置视窗大小及位置
        GLES30.glViewport(0, 0, mFrameBufferWidth, mFrameBufferHeight);
        //绑定帧缓冲id
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferPointer);
        //清除深度缓冲与颜色缓冲
        if (!mFrameBufferClean && !mFrameBufferCleanOnce) { //实现渲染画面叠加
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            mFrameBufferCleanOnce = true;
        }
        if (mFrameBufferClean) {
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            GLES30.glUniform1i(mGLFrameBufferProgramFunChoicePointer, 0); //第一次加载选择纹理方式渲染
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
            GLES30.glUniform1f(mFrameCountPointer, (float) (mFrameCount++));
            mPointBuf.position(0);
            mColorBuf.position(0);
            GLES30.glUniform1i(GLES30.glGetUniformLocation(mFrameBufferDrawProgram, "sTexture"), 0); //获取纹理属性的指针
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(mGLFrameObjectPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(mGLFrameObjectVertColorArrayPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            //将顶点纹理坐标数据传送进渲染管线
            GLES30.glVertexAttribPointer(mGLFrameVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
            GLES30.glEnableVertexAttribArray(mGLFrameObjectPositionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);  //启用颜色属性
            GLES30.glEnableVertexAttribArray(mGLFrameVTexCoordPointer);  //启用纹理采样定位坐标
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenTextureId);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(mGLFrameObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameVTexCoordPointer);
            if (mFrameCount > 1) {
                GLES30.glUniform1i(mGLFrameBufferProgramFunChoicePointer, 2); //选择各种绘制处理函数
                GLES30.glUniform2fv(mGLFrameTargetXYPointer, 1, new float[]{mEventX, mEventY}, 0);
                GLES30.glUniform1f(mGLFrameEffectRPointer, 0.1f); //设置作用半径
            }
        }
        //绑会系统默认framebuffer，否则会显示不出东西
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);//绑定帧缓冲id
    }

    /**触摸事件**/
    public void onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                mEventX = event.getX();
                mEventY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                GLES30.glUniform1i(mGLFrameBufferProgramFunChoicePointer, -1); //什么都不绘制，保留痕迹
                break;
        }
    }

    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        //先绘制内容到frambuffer
        drawToFrameBuffer(cameraMatrix, projMatrix);
        //绘制
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);//绑定帧系统默认缓冲id
        GLES30.glUseProgram(mBaseProgram);
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
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mFrameBufferTexturePointer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(mObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameObjectVertColorArrayPointer);
            GLES30.glDisableVertexAttribArray(mGLFrameVTexCoordPointer);
        }
//        super.drawTo(programID, positionPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
    }


    public void destroy() {
        if (!mIsDestroyed) {
            destroyFrameBuffer();
            //去除特殊shader程序
            destroyShader(mFrameBufferDrawProgram, mWaveVertexShaderPointer, mWaveFragShaderPointer);
            GLES30.glDeleteTextures(1, new int[] {mGenTextureId}, 0); //销毁纹理,gen和delete要成对出现
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
