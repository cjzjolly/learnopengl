package com.cjztest.gldrawlinesByMultiVectors;

import android.graphics.PointF;
import android.opengl.GLES30;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;

/**todo 使用包络方式重新设计**/
public class GLLineWithBezier {
    /**顶点字节数组**/
    private ByteBuffer mPointByteBuffer;
    /**顶点RGBA字节数组**/
    private ByteBuffer mColorByteBuffer;
    /**线头顶点字节数组**/
    private ByteBuffer mHeadCapPointByteBuffer;
    /**线头顶点RGBA字节数组**/
    private ByteBuffer mHeadCapColorByteBuffer;
    /**顶点坐标数组**/
    private FloatBuffer mPointBuf = null;
    /**顶点RGBA数组**/
    private FloatBuffer mColorBuf = null;
    /**线头顶点坐标数组**/
    private FloatBuffer mHeadPointBuf = null;
    /**线头顶点RGBA数组**/
    private FloatBuffer mHeadColorBuf = null;
    /**正在写入第几个顶点float**/
    private int mPointBufferPos = 0;
    /**正在写入第几个颜色float**/
    private int mColorBufferPos = 0;
    /**HeadCap正在写入第几个顶点float**/
    private int mHeadCapPointBufferPos = 0;
    /**HeadCap正在写入第几个颜色float**/
    private int mHeadCapColorBufferPos = 0;
    /**初始化时的顶点数目**/
    private int mInitVertexCount = 12;
    /**初始化时的颜色单元数目**/
    private int mInitColorCount = 16;
    /**HeadCap初始化时的顶点数目**/
    private int mHeadInitVertexCount = 12;
    /**HeadCap初始化时的颜色单元数目**/
    private int mHeadInitColorCount = 16;

    /**线条宽度**/
    private float mLineWidth = 0.05f;
    /**标准向量，用来确认端点的旋转量**/
    private float mStandardVec[] = new float[] {0, 1, 0};
    /**上一次做旋转计算用过的坐标**/
    private float mPrevInputVec[] = null;

    private float mPrevRotatedVec[] = null;

    /**上上次传入的坐标**/
    private float mBezierKeyPoint0[] = null;
    private float mBezierKeyPoint1[] = null;
    private boolean mIsLineCapHeadDrew = false;
    private boolean mIsLineCapEndDrew = false;
    private float mPrevPressure = Float.MIN_VALUE;
    private double mPrevDistance = Float.MIN_VALUE;

    /**是否模仿钢笔书写的模式**/
    public enum PenStyle {
        NORMAL,
        BY_ACC, //以加速度作为笔锋判断。
        BY_DEV_PRESSURE //以设备压力为判断
    }

    public void setPenStyle(PenStyle penStyle) {
        this.mPenStyle = penStyle;
    }

    private PenStyle mPenStyle = PenStyle.NORMAL;


    private Object mLock = new Object();
    private int endCapPointCount;


    public GLLineWithBezier() {
    }

    /**距离计算**/
    private double distance(float point0[], float point1[]) {
        return Math.sqrt(Math.pow(point0[0] - point1[0], 2) + Math.pow(point0[1] - point1[1], 2));
    }

    /**二次贝塞尔**/
    private List<PointF> bezierCalc(PointF[] keyPointP) {
        List<PointF> points = new LinkedList<>();
        double t = 0.1f; //步进
        for (double k = 0; k <= 1f; k += t) {
            double r = 1 - k;
            double x = Math.pow(r, 2) * keyPointP[0].x + 2 * k * r * keyPointP[1].x
                    + Math.pow(k, 2) * keyPointP[2].x;
            double y = Math.pow(r, 2) * keyPointP[0].y + 2 * k * r * keyPointP[1].y
                    + Math.pow(k, 2) * keyPointP[2].y;
            points.add(new PointF((float) x, (float) y));
        }
        return points;
    }

    public void setLineWidth(float lineWidth) {
        this.mLineWidth = lineWidth;
    }


    private int mPrevAngle = -1;
    /**对向量继续进行旋转 todo 要防止角度特别太厉害**/
    private float[] rotate2d(float vec[], double angle, double moveDistance) throws Exception {
        if (null == vec) {
            return null;
        }
        if (vec.length > 2) {
            throw new Exception("不接受超过2D的坐标");
        }
        int intAngle = (int) angle % 360;
        if (mPrevAngle != -1) {
            //todo 如何确定怎样的移动距离是不能用怎样的偏转度数呢
        }
        mPrevAngle = intAngle;
        Log.i("cjztest", "andgle:" + intAngle + ", moveDistance:" + moveDistance);
        double angleRad = Math.toRadians(intAngle);
        float rotatedVec[] = new float[2];
        rotatedVec[0] = (float) (Math.cos(angleRad) * vec[0] - Math.sin(angleRad) * vec[1]);
        rotatedVec[1] = (float) (Math.sin(angleRad) * vec[0] + Math.cos(angleRad) * vec[1]);
        return rotatedVec;
    }


    private float mPrevVec[] = null;
    /**给线头添加符合线宽的边界，便于和纤体本身链接**/
    private void lineCapAddBorder(double angle, float firstVec[], List<float[]> newVecs, float width) {
        try {
            if (mPrevVec == null) {
                mPrevVec = firstVec;
            }
            float rotatedVec0[] = rotate2d(new float[] {-width / 2f, 0}, angle + 180, distance(mPrevVec, firstVec));
            float rotatedVec1[] = rotate2d(new float[] {width / 2f, 0}, angle + 180, distance(mPrevVec, firstVec));
            float newVec[] = new float[6];
            if (rotatedVec0 == null || rotatedVec1 == null) {
                return;
            }
            //偏移到对应位置
            newVec[0] = rotatedVec0[0] + firstVec[0];
            newVec[1] = rotatedVec0[1] + firstVec[1];
            newVec[3] = rotatedVec1[0] + firstVec[0];
            newVec[4] = rotatedVec1[1] + firstVec[1];
            newVecs.add(newVec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**绘制线头
     * @param isHead 是否曲线头部添加线帽，否则视为曲线尾部添加线帽**/
    private int lineCap(boolean isHead, @NonNull float firstVec[], @NonNull float secVec[], int color, float width) {
        if (null == firstVec) {
            return -1;
        }
        if (mHeadPointBuf == null) {
            mHeadCapPointByteBuffer = ByteBuffer.allocateDirect(mHeadInitVertexCount * 4);    //顶点数 * sizeof(float)
            mHeadCapPointByteBuffer.order(ByteOrder.nativeOrder());
            mHeadPointBuf = mHeadCapPointByteBuffer.asFloatBuffer();
            mHeadPointBuf.position(0);
            mHeadCapPointBufferPos = 0;
        }
        //按初始化大小初始化RGBA字节数组和RGBA数组
        if (mHeadColorBuf == null) {
            mHeadCapColorByteBuffer = ByteBuffer.allocateDirect(mHeadInitColorCount * 4);
            mHeadCapColorByteBuffer.order(ByteOrder.nativeOrder());
            mHeadColorBuf = mHeadCapColorByteBuffer.asFloatBuffer();
            mHeadColorBuf.position(0);
            mHeadCapColorBufferPos = 0;
        }
        /**1、了解线条开始的方向，将半径线条绕旋转该方向与标准测量用向量的夹角的角度量
         * 2、旋转180度时按照一定步进产生多个顶点，todo 但怎么确定旋转的方向是顺时针还是逆时针？以什么为依据判断？以传入向量方向为参考，但具体怎么做？*/
        float initVert[] = new float[] { //初始时左端点的坐标，初始时在原点两侧，然后以传入的顶点作为偏移量
                -width / 2f, 0
        };
        //旋转并在过程中产生顶点
        float actualVec[] = new float[3];
        actualVec[0] = secVec[0] - firstVec[0];
        actualVec[1] = secVec[1] - firstVec[1];
//        if (Math.abs(actualVec[0]) < 0.0001f && Math.abs(actualVec[1]) < 0.0001f) {        //todo 如果相减之后遇到(0,0)向量怎么办呢？只能出现这种状况的向量不让它传入了
//            Log.e("cjztest", "fuck");
//        }
        double angle = calcAngleOfVectorsOnXYPanel(mStandardVec, actualVec); //对比基准向量旋转了多少度
        int step = 6; //改成只有90度可以得到一个尖头笔帽
        List<float[]> newVecs = new LinkedList<>();

        if (!isHead) {
            //给曲线结尾加一段和线宽等长的边
            lineCapAddBorder(angle, firstVec, newVecs, width);
        }

        //半圆线头
//        for (double degreeBias = 180 + angle; degreeBias >= 0 + angle; degreeBias -= step) {
        for (double degreeBias = angle; degreeBias <= 180 + angle; degreeBias += step) {
                try {
                float rotatedVec[] = rotate2d(initVert, degreeBias, 0);
                float newVec[] = new float[6];
                //偏移到对应位置
                newVec[0] = rotatedVec[0] + firstVec[0];
                newVec[1] = rotatedVec[1] + firstVec[1];
                newVec[3] += firstVec[0];
                newVec[4] += firstVec[1];
                newVecs.add(newVec);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (isHead) {
            //给曲线开头加一段和线宽等长的边
            lineCapAddBorder(angle, firstVec, newVecs, width);
        }

        for (float[] newVec : newVecs) {
            for (int i = 0; i < newVec.length; i++) {
                checkCapacity();
                mPointBuf.put(mPointBufferPos++, newVec[i]);
            }
            for (int i = 0; i < newVec.length / 3; i++) {
                checkCapacity();
                //写入颜色值r,g,b,a
//                int color = 0xFFFF0000;  //argb to abgr
                float alpha = (float) (((color & 0xFF000000) >> 24) & 0x000000FF) / 255f;
                float blue = (float) ((color & 0x000000FF)) / 255f;
                float green = (float) ((color & 0x0000FF00) >> 8) / 255f;
                float red = (float) ((color & 0x00FF0000) >> 16) / 255f;
                mColorBuf.put(mColorBufferPos++, red);
                mColorBuf.put(mColorBufferPos++, green);
                mColorBuf.put(mColorBufferPos++, blue);
                mColorBuf.put(mColorBufferPos++, alpha);
            }
        }
        return newVecs.size() * newVecs.get(0).length;
    }

    /**添加一系列触摸点，转换为指定粗细的线条**/
    public void addPoint(float x, float y, int colorARGB, float pressure, float maxPressure) {
        //按初始化大小初始化顶点字节数组和顶点数组
        synchronized (mLock) {
            if (null == mBezierKeyPoint0) {
                mBezierKeyPoint0 = new float[] {x, y, 0};
                return;
            }
            if (null == mBezierKeyPoint1) {
                mBezierKeyPoint1 = new float[] {x, y, 0};
                return;
            }
//            double distance = distance(new float[] {x, y}, mBezierKeyPoint1);
//            if (distance < 0.02f) { //太小的移动这次就不纳入顶点了
//                return;
//            }

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

            //通过贝塞尔曲线细化顶点
            PointF keyPoint0 = new PointF((mBezierKeyPoint0[0] + mBezierKeyPoint1[0]) / 2f, (mBezierKeyPoint0[1] + mBezierKeyPoint1[1]) / 2f);
            PointF keyPoint1 = new PointF(mBezierKeyPoint1[0], mBezierKeyPoint1[1]);
            PointF keyPoint2 = new PointF((x + mBezierKeyPoint1[0]) / 2f, (y + mBezierKeyPoint1[1]) / 2f);
            List<PointF> points = bezierCalc(new PointF[] {keyPoint0, keyPoint1, keyPoint2});

            double distance = distance(new float[] {x, y}, mBezierKeyPoint1);
            for (int i = 0; i < points.size(); i++) {
                PointF pointF = points.get(i);
                switch (mPenStyle) {
                    default:
                    case NORMAL: {
                        addPointToBuffer(pointF.x, pointF.y, colorARGB, mLineWidth);
                        break;
                    }
                    //加速度笔锋效果实验:
                    case BY_ACC: { //todo 这个以后做成根据设备、用户设置可调的才行
                        double ratio = 1f;
                        if (mPrevDistance == Float.MIN_VALUE) {
                            ratio = Math.min(1f, Math.pow(1f / (distance * 20), 1));
                        } else {
                            float delta = (float) ((distance - mPrevDistance) / points.size());
                            ratio = Math.min(1f, Math.pow(1f / ((mPrevDistance + delta * i) * 20), 1));
                        }
                        float width = (float) (mLineWidth * ratio);
                        addPointToBuffer(pointF.x, pointF.y, colorARGB, width);
                        break;
                    }
                    //设备笔锋效果实验:
                    case BY_DEV_PRESSURE: {
                        double ratio = 1f;
                        if (mPrevPressure == Float.MIN_VALUE) {
                            ratio = Math.min(1f, pressure / maxPressure);
                        } else {
                            float delta = (pressure - mPrevPressure) / points.size();
                            ratio = Math.min(1f, (mPrevPressure + delta * i) / maxPressure);
                        }
                        float width = (float) (mLineWidth * ratio);
                        addPointToBuffer(pointF.x, pointF.y, colorARGB, width);
                        break;
                    }
                }
            }
            mPrevDistance = distance; //记录上一次的距离值
            mPrevPressure = pressure; //记录上一次的压力值
            mBezierKeyPoint0 = new float[] {mBezierKeyPoint1[0], mBezierKeyPoint1[1]};
            mBezierKeyPoint1 = new float[] {x, y};
        }
    }

    private void addPointToBuffer(float x, float y, int colorARGB, float width) {
        checkCapacity();
        float initVert[] = new float[] { //初始时左右两端点的坐标，初始时在原点两侧，然后以传入的顶点作为偏移量
                -width / 2f, 0, 0,
                width / 2f, 0, 0,
        };
//        //自动缩放笔划粗细
//        float ratio = (float) Math.min(1.5f, (0.01f / distance * 5f));
//        initVert[0] = (float) (initVert[0] * ratio);
//        initVert[3] = (float) (initVert[3] * ratio);

        if (null == mPrevInputVec) {
            mPrevInputVec = new float[] {x, y, 0};
            return;
        }

        //添加线头，只执行一次
        if (!mIsLineCapHeadDrew) {
            lineCap(true, mPrevInputVec, new float[] {x, y, 0}, colorARGB, width);
            mIsLineCapHeadDrew = true;
        }
        //添加线段
        float dirVec[] = new float[] {x - mPrevInputVec[0], y - mPrevInputVec[1], 0 - mPrevInputVec[2]}; //把这次输入的向量-上次输入的向量，得到绘制移动方向的向量
        double angle = calcAngleOfVectorsOnXYPanel(mStandardVec, dirVec); //旋转角度
        //todo 如果旋转角度产生投影
        float vert[] = new float[6];
        try {
            float rotatedVec[] = rotate2d(new float[] {initVert[0], initVert[1]}, angle, 0);
            vert[0] = rotatedVec[0];
            vert[1] = rotatedVec[1];
            rotatedVec = rotate2d(new float[] {initVert[3], initVert[4]}, angle, 0);
            vert[3] = rotatedVec[0];
            vert[4] = rotatedVec[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        vert[0] += mPrevInputVec[0];
        vert[1] += mPrevInputVec[1];
        vert[3] += mPrevInputVec[0];
        vert[4] += mPrevInputVec[1];




        /*todo 上一次端点和这次端点是否重叠**/
        if (mPrevRotatedVec != null) {

        }
        mPrevRotatedVec = vert;



        //消除上一次的线头
        if (mIsLineCapEndDrew) {
            mPointBufferPos -= endCapPointCount;
            mColorBufferPos -= endCapPointCount / 3 * 4;
        }



        //写入坐标值
        for (int i = 0; i < vert.length; i++) {
            checkCapacity();
            mPointBuf.put(mPointBufferPos++, vert[i]);
        }
        for (int i = 0; i < vert.length / 3; i++) {
            //写入颜色值r,g,b,a
            int color = colorARGB;  //argb to abgr
//            int color = Color.GREEN;  //cjztest
//            if (i == 1) {
//                color = Color.YELLOW; //cjztest
//            }
            float alpha = (float) (((color & 0xFF000000) >> 24) & 0x000000FF) / 255f;
            float blue = (float) ((color & 0x000000FF)) / 255f;
            float green = (float) ((color & 0x0000FF00) >> 8) / 255f;
            float red = (float) ((color & 0x00FF0000) >> 16) / 255f;
            mColorBuf.put(mColorBufferPos++, red);
            mColorBuf.put(mColorBufferPos++, green);
            mColorBuf.put(mColorBufferPos++, blue);
            mColorBuf.put(mColorBufferPos++, alpha);
        }
        checkCapacity();
        //添加线尾，每次清除上一次的线尾，然后增加一次新的
        endCapPointCount = lineCap(false, new float[] {x, y, 0}, mPrevInputVec, colorARGB, width);
        mIsLineCapEndDrew = true;
        checkCapacity();

        mPrevInputVec = new float[] {x, y, 0};
    }


    /**todo 通过斜率判断两个线段是否相交**/
    private void intersectCheck(float line0X, float line0Y, float line1X, float line1Y) {
//        float k0 =
    }

    private void checkCapacity() {
        //如果写入的颜色数超过初始值，将顶点数和颜色数组容量翻倍
        if (mPointBufferPos * 3 / 2 >= mInitVertexCount) {
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

    //XY平面上的的旋转量
    private double calcAngleOfVectorsOnXYPanel(float vec0[], float vec1[]) {
        double distanceOfVec0 = Math.sqrt(Math.pow(vec0[0], 2) + Math.pow(vec0[1], 2));
        double distanceOfVec1 = Math.sqrt(Math.pow(vec1[0], 2) + Math.pow(vec1[1], 2));
        double dotProduct = vec0[0] * vec1[0] + vec0[1] * vec1[1];
        double angle = Math.toDegrees(Math.acos(dotProduct / (distanceOfVec0 * distanceOfVec1)));
        if (vec1[0] > 0) {
            angle = 360f - angle;
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

    /**把绘制过程迁移回去对象内部**/
    public void draw(int vertPointer, int colorPointer) {
        FloatBuffer lineVerts = mPointBuf;
        FloatBuffer colors = mColorBuf;
        if (lineVerts != null && colors != null) {
            lineVerts.position(0);
            colors.position(0);
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer
                    (
                            vertPointer,
                            3,
                            GLES30.GL_FLOAT,
                            false,
                            0,  //stride是啥？
                            lineVerts
                    );
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer
                    (
                            colorPointer,
                            4,
                            GLES30.GL_FLOAT,
                            false,
                            0,
                            colors
                    );
            GLES30.glEnableVertexAttribArray(vertPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(colorPointer);  //启用颜色属性

//            GLES30.glDrawArrays(GLES30.GL_LINES, 0, getPointBufferPos() / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
//            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, getPointBufferPos() / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
//            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ZERO); //可以解决线条自身重叠问题
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, getPointBufferPos() / 3); //cjztest
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA); //还原
            GLES30.glDisableVertexAttribArray(vertPointer); //启用顶点属性
            GLES30.glDisableVertexAttribArray(colorPointer);  //启用颜色属性
        }
    }
}