package com.cjztest.glShaderEffect;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLLine extends GLObject {
    /**
     * 顶点字节数组
     **/
    protected ByteBuffer mPointByteBuffer;
    /**
     * 顶点RGBA字节数组
     **/
    protected ByteBuffer mColorByteBuffer;
    /**
     * 顶点坐标数组
     **/
    protected FloatBuffer mPointBuf = null;
    /**
     * 顶点RGBA数组
     **/
    protected FloatBuffer mColorBuf = null;
    /**
     * 正在写入第几个顶点float
     **/
    protected int mPointBufferPos = 0;
    /**
     * 正在写入第几个颜色float
     **/
    private int mColorBufferPos = 0;
    /**
     * 初始化时的顶点数目
     **/
    private int mInitVertexCount = 12;
    /**
     * 初始化时的颜色单元数目
     **/
    private int mInitColorCount = 16;

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
            mColorByteBuffer = ByteBuffer.allocateDirect(mInitColorCount * 4);
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
        if (mPointBufferPos >= mInitVertexCount) {//todo bug，扩容之后有些点信息错了
            Log.i("GLLines", "扩容点数到:" + mInitVertexCount);
            mInitVertexCount += 12;
            mInitColorCount += 16;

            ByteBuffer qbb = ByteBuffer.allocateDirect(mInitVertexCount * 4);    //顶点数 * sizeof(float) ; 加4个点
            qbb.order(ByteOrder.nativeOrder());
            System.arraycopy(mPointByteBuffer.array(), 0, qbb.array(), 0, (mPointBufferPos + 1) * 4);   //顶点数 * sizeof(float)
            mPointByteBuffer = qbb;
            mPointBuf = mPointByteBuffer.asFloatBuffer();
            mPointBuf.position(0);

            ByteBuffer qbb2 = ByteBuffer.allocateDirect(mInitColorCount * 4);    //顶点数 * sizeof(float) ;
            qbb2.order(ByteOrder.nativeOrder());
            System.arraycopy(mColorByteBuffer.array(), 0, qbb2.array(), 0, (mColorBufferPos + 1) * 4);  //sizeof(R,G,B,Alpha) * sizeof(float)
            mColorByteBuffer = qbb2;
            mColorBuf = mColorByteBuffer.asFloatBuffer();
            mColorBuf.position(0);

        }
    }

    protected void clearVertxAndColorBuffer() {
        mInitVertexCount = 12;
        mInitColorCount = 16;
        mPointBuf.clear();
        mPointBufferPos = 0;
        mPointByteBuffer.clear();
        mColorBuf.clear();
        mColorBufferPos = 0;
        mColorByteBuffer.clear();
    }

    @Override
    public void drawTo(int programID, int positionPointer, int vTexCoordPointer, int colorPointer, float[] cameraMatrix, float[] projMatrix, int muMVPMatrixPointer, int glFunChoicePointer) { //安卓的GLES30类中已经有主线程创建的EGL context，直接用就好
        //step 0:确认要怎样变换，也就是确定变换关系（平移、旋转、缩放）矩阵
        locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
        if (mPointBuf != null && mColorBuf != null) {
            GLES30.glUniform1i(glFunChoicePointer, 0); //选择线条渲染方式
            //step 1:传入物体坐标和颜色，由gl根据上面的变换关系放到目标位置，并赋予颜色
            mPointBuf.position(0);
            mColorBuf.position(0);
            GLES30.glLineWidth(3f);
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(positionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf); //stride是啥？
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(colorPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            GLES30.glEnableVertexAttribArray(positionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(colorPointer);  //启用颜色属性
            GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, mPointBufferPos / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
            GLES30.glDisableVertexAttribArray(positionPointer);
            GLES30.glDisableVertexAttribArray(colorPointer);
        }
    }
}
