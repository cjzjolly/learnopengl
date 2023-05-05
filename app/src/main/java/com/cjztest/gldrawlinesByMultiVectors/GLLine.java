package com.cjztest.gldrawlinesByMultiVectors;

import android.graphics.Color;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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
    private int mInitVertexCount = 12;
    /**初始化时的颜色单元数目**/
    private int mInitColorCount = 16;

    /**线条宽度**/
    private float mLineWidth = 0.1f;
    /**标准向量，用来确认端点的旋转量**/
    private float mStandardVec[] = new float[] {0, 1, 0};
    /**上一次传入的坐标**/
    private float mPrevInputVec[] = new float[3];


    private Object mLock = new Object();


    public GLLine() {
    }

    private double distance(float point0[], float point1[]) {
        return Math.sqrt(Math.pow(point0[0] - point1[0], 2) + Math.pow(point0[1] - point1[1], 2));
    }

    public void addPoint(float x, float y, int colorARGB) {
        //按初始化大小初始化顶点字节数组和顶点数组
        synchronized (mLock) {
            if (distance(new float[] {x, y}, mPrevInputVec) < 0.01f) { //太小的移动这次就不纳入顶点了
                return;
            }
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


            //核心代码:把这次输入的向量-上次输入的向量，得到绘制移动方向的向量，对比作为标准的向量，计算两端点坐标旋转角度：
            float initVert[] = new float[]{ //初始时左右两端点的坐标，初始时在原点两侧，然后以传入的顶点作为偏移量
                    -mLineWidth / 2f, 0, 0,
                    mLineWidth / 2f, 0, 0,
            };
            float newVec[] = new float[] {x - mPrevInputVec[0], y - mPrevInputVec[1], 0 - mPrevInputVec[2]}; //把这次输入的向量-上次输入的向量，得到绘制移动方向的向量
            double angle = calcAngleOfVectorsOnXYPanel(mStandardVec, newVec);
            double angleRad = Math.toRadians(angle);
            float vert[] = new float[6];
            vert[0] = (float) (Math.cos(angleRad) * initVert[0] - Math.sin(angleRad) * initVert[1]);
            vert[1] = (float) (Math.sin(angleRad) * initVert[0] + Math.cos(angleRad) * initVert[1]);
            vert[3] = (float) (Math.cos(angleRad) * initVert[3] - Math.sin(angleRad) * initVert[4]);
            vert[4] = (float) (Math.sin(angleRad) * initVert[3] + Math.cos(angleRad) * initVert[4]);
            vert[0] += x;
            vert[1] += y;
            vert[3] += x;
            vert[4] += y;
            mPrevInputVec = new float[] {x, y, 0};


            //写入坐标值
            for (int i = 0; i < vert.length; i++) {
                mPointBuf.put(mPointBufferPos++, vert[i]);
            }
            for (int i = 0; i < vert.length / 3; i++) {
                //写入颜色值r,g,b,a
//                int color = colorARGB;  //argb to abgr
                int color = Color.RED;  //cjztest
                if (i == 1) {
                    color = Color.GREEN; //cjztest
                }
                float alpha = (float) (((color & 0xFF000000) >> 24) & 0x000000FF) / 255f;
                float blue = (float) ((color & 0x000000FF)) / 255f;
                float green = (float) ((color & 0x0000FF00) >> 8) / 255f;
                float red = (float) ((color & 0x00FF0000) >> 16) / 255f;
                mColorBuf.put(mColorBufferPos++, red);
                mColorBuf.put(mColorBufferPos++, green);
                mColorBuf.put(mColorBufferPos++, blue);
                mColorBuf.put(mColorBufferPos++, alpha);
            }
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
    }

    //XY平面上的的旋转量
    private double calcAngleOfVectorsOnXYPanel(float vec0[], float vec1[]) {
        double distanceOfVec0 = Math.sqrt(Math.pow(vec0[0], 2) + Math.pow(vec0[1], 2));
        double distanceOfVec1 = Math.sqrt(Math.pow(vec1[0], 2) + Math.pow(vec1[1], 2));
        double dotProduct = vec0[0] * vec1[0] + vec0[1] * vec1[1];
        double angle = Math.toDegrees(Math.acos(dotProduct / (distanceOfVec0 * distanceOfVec1)));
        if (vec1[0] > 0) {
            angle = 360 - angle;
        }
        return angle;
    }

    /**获取线条的顶点坐标集**/
    public FloatBuffer getPointBuf() {
        synchronized (mLock) {
            return mPointBuf;
        }
    }

    /**获取线条的顶点坐标集**/
    public FloatBuffer getColorBuf() {
        synchronized (mLock) {
            return mColorBuf;
        }
    }


    public int getPointBufferPos() {
        return mPointBufferPos;
    }
}