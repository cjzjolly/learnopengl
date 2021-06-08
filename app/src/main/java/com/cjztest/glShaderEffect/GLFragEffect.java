package com.cjztest.glShaderEffect;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

public class GLFragEffect extends GLLine {
    private final float mX;
    private final float mY;
    private final float mZ;
    private final float mWidth;
    private final float mHeight;

    FloatBuffer mTexCoorBuffer;//顶点纹理坐标数据缓冲
    private static Map<Integer, Integer> mMapIndexToTextureID = new HashMap<>(); //(纹理id，纹理数)
    private int mGenTextureId = 0;
    private boolean mIsDestroyed = false;
    private int mAlpha;

    public GLFragEffect(float x, float y, float z, float w, float h) {
        this.mX = x;
        this.mY = y;
        this.mZ = z;
        this.mWidth = w;
        this.mHeight = h;
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

    }

    @Override
    public void drawTo(int programID, int positionPointer, int vTexCoordPointer, int colorPointer, float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer, int glFunChoicePointer) {
        if (mIsDestroyed) {
            return;
        }
        locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
        if (mPointBuf != null && mColorBuf != null) {
            GLES30.glEnable(GL10.GL_LIGHTING);
            GLES30.glUniform1i(glFunChoicePointer, 2);
            mPointBuf.position(0);
            mColorBuf.position(0);
//            GLES30.glUniform1i(GLES30.glGetUniformLocation(programID, "sTexture"), 0); //获取纹理属性的指针
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(positionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //三维向量，size为2
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(colorPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            //将顶点纹理坐标数据传送进渲染管线
            GLES30.glVertexAttribPointer(vTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);  //二维向量，size为2
            GLES30.glEnableVertexAttribArray(positionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(colorPointer);  //启用颜色属性
            GLES30.glEnableVertexAttribArray(vTexCoordPointer);  //启用纹理采样定位坐标
//            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mGenTextureId);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(positionPointer);
            GLES30.glDisableVertexAttribArray(colorPointer);
            GLES30.glDisableVertexAttribArray(vTexCoordPointer);
        }
//        super.drawTo(programID, positionPointer, colorPointer, cameraMatrix, projMatrix, muMVPMatrixPointer, glFunChoicePointer);
    }

    public void destroy() {
        if (!mIsDestroyed) {
            GLES30.glDeleteTextures(1, new int[] {mGenTextureId}, 0); //销毁纹理,gen和delete要成对出现
            mMapIndexToTextureID.put(mGenTextureId, null); //让该id可以复用
            Log.i("GLImage", "clean texture:" + mGenTextureId);
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
