package com.cjztest.gldrawline;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class GLLine {
    /**顶点字节数组**/
    private ByteBuffer mPointByteBuffer;
    /**顶点RGBA字节数组**/
    private ByteBuffer mColorByteBuffer;
    /**顶点坐标数组**/
    private FloatBuffer mPointBuf = null;
    /**顶点RGBA数组**/
    private FloatBuffer mColorBuf = null;
    /**正在写入第几个顶点float**/
    private int mPointBufferPos = 0;
    /**正在写入第几个颜色float**/
    private int mColorBufferPos = 0;
    /**初始化时的顶点数目**/
    private int mInitVertexCount = 1 * 16;

    public void addPoint(float x, float y, int colorARGB) {
        addPoint(x, y, 0, colorARGB);
    }

    public void addPoint(float x, float y, float z, int colorARGB) {
        //按初始化大小初始化顶点字节数组和顶点数组
        if (mPointBuf == null) {
            mPointByteBuffer = ByteBuffer.allocateDirect(mInitVertexCount * 4);    //顶点数 * sizeof(float)
            mPointByteBuffer.order(ByteOrder.nativeOrder());
            mPointBuf = mPointByteBuffer.asFloatBuffer();
            mPointBuf.position(0);
            mPointBufferPos = 0;
        }
        //按初始化大小初始化RGBA字节数组和RGBA数组
        if (mColorBuf == null) {
            mColorByteBuffer = ByteBuffer.allocateDirect(mInitVertexCount * 4);
            mColorByteBuffer.order(ByteOrder.nativeOrder());
            mColorBuf = mColorByteBuffer.asFloatBuffer();
            mColorBuf.position(0);
            mColorBufferPos = 0;
        }
        //写入坐标值x,y,z
        mPointBuf.put(mPointBufferPos++, x);
        mPointBuf.put(mPointBufferPos++, y);
        mPointBuf.put(mPointBufferPos++, z);
        //写入颜色值r,g,b,a
        int color = colorARGB;  //argb to abgr
//        Log.i("GLLineColor", String.format("Color:0x%8X", color));
        float alpha = (float) (((color & 0xFF000000) >> 24) & 0x000000FF) / 255f;
        float blue = (float) ((color & 0x000000FF)) / 255f;
        float green = (float) ((color & 0x0000FF00) >> 8) / 255f;
        float red = (float) ((color & 0x00FF0000) >> 16) / 255f;
        mColorBuf.put(mColorBufferPos++, red);
        mColorBuf.put(mColorBufferPos++, green);
        mColorBuf.put(mColorBufferPos++, blue);
        mColorBuf.put(mColorBufferPos++, alpha);
//        Log.i("GLLineColor", String.format("r:%f,g:%f,b:%f,a:%f", red, green, blue, alpha));

        //如果写入的颜色数超过初始值，将顶点数和颜色数组容量翻倍
        if (mColorBufferPos * 4 >= mInitVertexCount) {
            Log.i("GLLines", "扩容点数到:" + mInitVertexCount);
            mInitVertexCount *= 2;

            ByteBuffer qbb = ByteBuffer.allocateDirect(mInitVertexCount * 4);    //顶点数 * sizeof(float) ;
            qbb.order(ByteOrder.nativeOrder());
            System.arraycopy(mPointByteBuffer.array(), 0, qbb.array(), 0, (mPointBufferPos) * 4);   //顶点数 * sizeof(float)
            mPointByteBuffer = qbb;
            mPointBuf = mPointByteBuffer.asFloatBuffer();

            ByteBuffer qbb2 = ByteBuffer.allocateDirect(mInitVertexCount * 4);    //顶点数 * sizeof(float) ;
            qbb2.order(ByteOrder.nativeOrder());
            System.arraycopy(mColorByteBuffer.array(), 0, qbb2.array(), 0, (mColorBufferPos) * 4);  //sizeof(R,G,B,Alpha) * sizeof(float)
            mColorByteBuffer = qbb2;
            mColorBuf = mColorByteBuffer.asFloatBuffer();

        }
    }

    public void drawTo(int positionPointer, int colorPointer) { //GLES30中已经有主线程创建的EGL context，直接用就好
        if (mPointBuf != null && mColorBuf != null) {
            Log.i("cjztest", "GLLine.drawTo");
            mPointBuf.position(0);
            mColorBuf.position(0);
            GLES30.glLineWidth(15f);
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer
                    (
                            positionPointer,
                            3,
                            GLES30.GL_FLOAT,
                            false,
                            0,  //stride是啥？
                            mPointBuf
                    );
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer
                    (
                            colorPointer,
                            4,
                            GLES30.GL_FLOAT,
                            false,
                            0,
                            mColorBuf
                    );
            GLES30.glEnableVertexAttribArray(positionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(colorPointer);  //启用颜色属性
            GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, mPointBufferPos / 3); //添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        }
    }
}
