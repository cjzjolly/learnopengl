package com.cjztest.glShaderEffect;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.cjztest.glShaderEffect.ShaderUtil.destroyShader;

/**Pixel Buffer PBO Demo**/
public class GLFrameBufferEffectPBODemo extends GLLine {
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
    private int[] mPixelBuffferPointerArray;

    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGenTextureId = 0;
    private int mImgByteSize;

    public GLFrameBufferEffectPBODemo(int baseProgramPointer, float x, float y, float z, float w, float h, int windowW, int windowH, Context context, int imgW, int imgH) {
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
        startBindEmptyTexture();
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

    private void startBindEmptyTexture() {
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
        mMapIndexToTextureID.put(mGenTextureId, 1); //使用该id作为纹理索引指针
    }

    /**
     * 创建2个framebuffer作为每次渲染结果的叠加专用纹理
     **/
    private void createPBO() {
        int FBOCount = 2;
        mImgByteSize = mImgWidth * mImgHeight * 4;
        mPixelBuffferPointerArray = new int[FBOCount];
        GLES30.glGenBuffers(FBOCount, mPixelBuffferPointerArray, 0);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPixelBuffferPointerArray[0]);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgByteSize,  null, GLES30.GL_STREAM_DRAW);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPixelBuffferPointerArray[1]);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mImgByteSize,  null, GLES30.GL_STREAM_DRAW);
    }

    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        if (mIsDestroyed) {
            return;
        }
        GLES30.glUseProgram(mBaseProgram);
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


//            https://zhuanlan.zhihu.com/p/115257287
//            https://www.jianshu.com/p/1fa36461fc6f?utm_campaign=hugo&utm_medium=reader_share&utm_content=note&utm_source=qq
            //调用 glTexSubImage2D 后立即返回，不影响 CPU 时钟周期
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenTextureId);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPixelBuffferPointerArray[mFrameCount % 2]);
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, mImgWidth, mImgHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            //更新图像数据，复制到 PBO 中
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPixelBuffferPointerArray[(mFrameCount + 1) % 2]);
            GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mImgByteSize, null, GLES30.GL_STREAM_DRAW);
            Buffer buf = GLES30.glMapBufferRange(GLES30.GL_PIXEL_UNPACK_BUFFER, 0, mImgByteSize, GLES30.GL_MAP_WRITE_BIT | GLES30.GL_MAP_INVALIDATE_BUFFER_BIT);
            ByteBuffer bytebuffer = ((ByteBuffer) buf).order(ByteOrder.nativeOrder());
            bytebuffer.position(0);
            byte pixels[] = new byte[mImgByteSize];
            Arrays.fill(pixels, (byte) (255 & 0xFF));
            bytebuffer.put(pixels);
            bytebuffer.position(0);
            //todo 填充像素
            GLES30.glUnmapBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);


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
