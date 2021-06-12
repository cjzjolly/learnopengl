package com.cjztest.glShaderEffect;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLCircle extends GLObject {
    private int mRoundColor;
    private int mCenterColor;
    private float mR = 0; //半径
    FloatBuffer mPointBuf;//顶点坐标数据缓冲
    FloatBuffer mColorBuf;//顶点着色数据缓冲
    int vCount = 0;

    public GLCircle(int baseProgramPointer, float r, int centerColorARGB, int roundColorARGB) {
        super(baseProgramPointer);
        mR = r;
        mCenterColor = centerColorARGB;
        mRoundColor = roundColorARGB;
        initVertexData();
    }

    //初始化顶点坐标与着色数据的方法
    public void initVertexData() {
        //顶点坐标数据的初始化================begin============================
        int n = 30;
        vCount = n + 2;

        float angdegSpan = 360.0f / n;
        float[] vertices = new float[vCount * 3];//顶点坐标数据数组
        //坐标数据初始化
        int count = 0;
        //第一个顶点的坐标
        vertices[count++] = 0;
        vertices[count++] = 0;
        vertices[count++] = 0;
        for (float angdeg = 0; Math.ceil(angdeg) <= 360; angdeg += angdegSpan) {//循环生成其他顶点的坐标
            double angrad = Math.toRadians(angdeg);//当前弧度
            //当前点
            vertices[count++] = (float) (-mR * Math.sin(angrad));//顶点x坐标
            vertices[count++] = (float) (mR * Math.cos(angrad));//顶点y坐标
            vertices[count++] = 0;//顶点z坐标
        }
        //创建顶点坐标数据缓冲
        //vertices.length*4是因为一个整数四个字节
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mPointBuf = vbb.asFloatBuffer();//转换为Float型缓冲
        mPointBuf.put(vertices);//向缓冲区中放入顶点坐标数据
        mPointBuf.position(0);//设置缓冲区起始位置
        //特别提示：由于不同平台字节顺序不同数据单元不是字节的一定要经过ByteBuffer
        //转换，关键是要通过ByteOrder设置nativeOrder()，否则有可能会出问题
        //顶点坐标数据的初始化================end============================

        //顶点颜色值数组，每个顶点4个色彩值RGBA
        count = 0;
        float colors[] = new float[vCount * 4];
        //圆心顶点的颜色
        float alpha = (float) (((mCenterColor & 0xFF000000) >> 24) & 0x000000FF) / 255f;
        float blue = (float) ((mCenterColor & 0x000000FF)) / 255f;
        float green = (float) ((mCenterColor & 0x0000FF00) >> 8) / 255f;
        float red = (float) ((mCenterColor & 0x00FF0000) >> 16) / 255f;
        colors[count++] = red;
        colors[count++] = green;
        colors[count++] = blue;
        colors[count++] = alpha;
        //剩余顶点的颜色:绿色
        for (int i = 4; i < colors.length; i += 4) {
            alpha = (float) (((mRoundColor & 0xFF000000) >> 24) & 0x000000FF) / 255f;
            blue = (float) ((mRoundColor & 0x000000FF)) / 255f;
            green = (float) ((mRoundColor & 0x0000FF00) >> 8) / 255f;
            red = (float) ((mRoundColor & 0x00FF0000) >> 16) / 255f;
            colors[count++] = red;
            colors[count++] = green;
            colors[count++] = blue;
            colors[count++] = alpha;
        }
        //创建顶点着色数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());//设置字节顺序
        mColorBuf = cbb.asFloatBuffer();//转换为Float型缓冲
        mColorBuf.put(colors);//向缓冲区中放入顶点着色数据
        mColorBuf.position(0);//设置缓冲区起始位置
        //特别提示：由于不同平台字节顺序不同数据单元不是字节的一定要经过ByteBuffer
        //转换，关键是要通过ByteOrder设置nativeOrder()，否则有可能会出问题
        //顶点着色数据的初始化================end============================
    }

    @Override
    public void drawTo(float[] cameraMatrix, float[] projMatrix) {
        GLES30.glUseProgram(mBaseProgram);
        //step 0:确认要怎样变换，也就是确定变换关系（平移、旋转、缩放）矩阵
        locationTrans(cameraMatrix, projMatrix, muMVPMatrixPointer);
        if (mPointBuf != null && mColorBuf != null) {
            GLES30.glUniform1i(mGLFunChoicePointer, 0); //选择线条渲染方式
            //step 1:传入物体坐标和颜色，由gl根据上面的变换关系放到目标位置，并赋予颜色
            mPointBuf.position(0);
            mColorBuf.position(0);
            GLES30.glLineWidth(3f);
            //将顶点位置数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mPointBuf);
            //将顶点颜色数据送入渲染管线
            GLES30.glVertexAttribPointer(mObjectVertColorArrayPointer, 4, GLES30.GL_FLOAT, false, 0, mColorBuf);
            GLES30.glEnableVertexAttribArray(mObjectPositionPointer); //启用顶点属性
            GLES30.glEnableVertexAttribArray(mObjectVertColorArrayPointer);  //启用颜色属性
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, vCount);
            GLES30.glDisableVertexAttribArray(mObjectPositionPointer);
            GLES30.glDisableVertexAttribArray(mObjectVertColorArrayPointer);
        }
    }
}
