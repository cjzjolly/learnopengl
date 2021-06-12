package com.cjztest.glShaderEffect;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import static com.cjztest.glShaderEffect.ShaderUtil.checkGlError;
import static com.cjztest.glShaderEffect.ShaderUtil.destroyShader;
import static com.cjztest.glShaderEffect.ShaderUtil.loadShader;

public class GLFragEffectLightPot extends GLLine {
    private final float mX;
    private final float mY;
    private final float mZ;
    private final float mWidth;
    private final float mHeight;
    private final float mWindowW;
    private final float mWindowH;
    private  Context mContext;

    FloatBuffer mTexCoorBuffer;//顶点纹理坐标数据缓冲
    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGenTextureId = 0;
    private boolean mIsDestroyed = false;
    private int mAlpha;
    private int mSeaFragShaderPointer;
    private int mSeaVertexShaderPointer;
    private int mSeaProgram;
    private int mObjectPositionPointer;
    private int mVTexCoordPointer;
    private int mObjectVertColorArrayPointer;
    private int muMVPMatrixPointer;
    private int mFrameCountPointer;
    private int mResoulutionPointer;
    private int mFrameCount = 0;

    public GLFragEffectLightPot(int baseProgramPointer,float x, float y, float z, float w, float h, int windowW, int windowH, Context context) {
        super(baseProgramPointer);
        this.mX = x;
        this.mY = y;
        this.mZ = z;
        this.mWidth = w;
        this.mHeight = h;
        this.mWindowW = windowW;
        this.mWindowH = windowH;
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
        startBindTexture();
        rotate(180, 0, 0, 1);
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

    private void startBindTexture() {
        //特殊纹理，需要专门加载其他程序:
        String fragShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/fragShaderLightPot.shader", mContext.getResources());
        String vertexShaderScript = ShaderUtil.loadFromAssetsFile("fragColorEffect1/vertShader.shader", mContext.getResources());
        //基于顶点着色器与片元着色器创建程序 step_0：编译脚本
        mSeaFragShaderPointer = loadShader(GLES30.GL_FRAGMENT_SHADER, fragShaderScript);
        mSeaVertexShaderPointer = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderScript);
        mSeaProgram = GLES30.glCreateProgram();
        //若程序创建成功则向程序中加入顶点着色器与片元着色器
        if (mSeaProgram != 0) { //step 1: 创建program后附加编译后的脚本
            //>>>>>>>>>>>>
            //向程序中加入顶点着色器
            GLES30.glAttachShader(mSeaProgram, mSeaFragShaderPointer);
            checkGlError("glAttachShader");
            //向程序中加入片元着色器
            GLES30.glAttachShader(mSeaProgram, mSeaVertexShaderPointer);
            checkGlError("glAttachShader");
            //链接程序
            GLES30.glLinkProgram(mSeaProgram);
            //<<<<<<<<<<<<
            //存放链接成功program数量的数组
            int[] linkStatus = new int[1];
            //获取program的链接情况
            GLES30.glGetProgramiv(mSeaProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
            //若链接失败则报错并删除程序
            if (linkStatus[0] != GLES30.GL_TRUE) { //step 2: 检查是否链接并附加成功，不成功要清理
                Log.e("ES30_ERROR", "Could not link program: ");
                Log.e("ES30_ERROR", GLES30.glGetProgramInfoLog(mSeaProgram));
                GLES30.glDeleteProgram(mSeaProgram);
                mSeaProgram = 0;
            }
        }
        //获取程序中顶点位置属性引用"指针"
        mObjectPositionPointer = GLES30.glGetAttribLocation(mSeaProgram, "objectPosition");
        //纹理采样坐标
        mVTexCoordPointer = GLES30.glGetAttribLocation(mSeaProgram, "vTexCoord");
        //获取程序中顶点颜色属性引用"指针"
        mObjectVertColorArrayPointer = GLES30.glGetAttribLocation(mSeaProgram, "objectColor");
        //获取程序中总变换矩阵引用"指针"
        muMVPMatrixPointer = GLES30.glGetUniformLocation(mSeaProgram, "uMVPMatrix");
        //渲染帧计数指针
        mFrameCountPointer = GLES30.glGetUniformLocation(mSeaProgram, "frame");
        //设置分辨率指针，告诉gl脚本现在的分辨率
        mResoulutionPointer = GLES30.glGetUniformLocation(mSeaProgram, "resolution");
    }

    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        //特殊shader效果，写在shader脚本上，所以不能用基本program，要更换为对应效果的Program
        GLES30.glUseProgram(mSeaProgram);
        //设置图像分辨率
        GLES30.glUniform2fv(mResoulutionPointer, 1, new float[] {mWindowW, mWindowH}, 0);
        locationTrans(cameraMatrix, projMatrix, this.muMVPMatrixPointer);
        if (mPointBuf != null && mColorBuf != null) {
            if (mFrameCount < 0) {
                mFrameCount = 0;
            }
            GLES30.glUniform1f(mFrameCountPointer, (float) (mFrameCount++));
            mPointBuf.position(0);
            mColorBuf.position(0);
//            GLES30.glUniform1i(GLES30.glGetUniformLocation(programID, "sTexture"), 0); //获取纹理属性的指针
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectVertColorArrayPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            //将顶点纹理坐标数据传送进渲染管线
            GLES30.glVertexAttribPointer(mVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
            GLES30.glEnableVertexAttribArray(mObjectPositionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(mObjectVertColorArrayPointer);  //启用颜色属性
            GLES30.glEnableVertexAttribArray(mVTexCoordPointer);  //启用纹理采样定位坐标
//            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenTextureId);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(mObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mObjectVertColorArrayPointer);
            GLES30.glDisableVertexAttribArray(mVTexCoordPointer);
        }
//        super.drawTo(programID, positionPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
    }


    public void destroy() {
        if (!mIsDestroyed) {
            //去除特殊shader程序
            destroyShader(mSeaProgram, mSeaVertexShaderPointer, mSeaFragShaderPointer);
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
