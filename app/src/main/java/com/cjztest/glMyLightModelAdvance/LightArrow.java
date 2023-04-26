package com.cjztest.glMyLightModelAdvance;

import android.content.res.Resources;
import android.opengl.GLES30;
import android.util.Log;

import com.book2.Sample6_3.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**光斑**/
public class LightArrow {
    private final Resources mRsc;
    private FloatBuffer mVertexBuffer;// 顶点坐标数据缓冲
    private FloatBuffer mUVBuffer;// 顶点坐标数据缓冲
    private String mVertexShader;// 顶点着色器代码脚本
    private String mFragmentShader;// 片元着色器代码脚本
    private int mProgram;
    private int muMVPMatrixHandle;
    private int mObjMatrixHandle;
    private int maPositionPointer;
    private float vertxData[] = { //箭头坐标
            0, 0, 0,
            0, 1, 0,
            -0.2f, 0.8f, 0,
            0.2f, 0.8f, 0,
            0f, 1f, 0,

    };

    /**起点坐标**/
    private float mStartVec[] = new float[3];

    /**终点坐标**/
    private float mEndVec[] = new float[3];

    public LightArrow(Resources resources, float screenRatio) {
        mRsc = resources;
        initShader();
        initVertx();
    }

    /**加载光点显示**/
    public void initShader() {
        // 加载顶点着色器的脚本内容
        mVertexShader = ShaderUtil.loadFromAssetsFile("cjztest/lightmodeladvance/vertex.shader",
                mRsc);
        // 加载片元着色器的脚本内容
        mFragmentShader = ShaderUtil.loadFromAssetsFile("cjztest/lightmodeladvance/fragShaderBase.shader",
                mRsc);
        // program索引
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //变换矩阵索引
        muMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        mObjMatrixHandle = GLES30.glGetUniformLocation(mProgram, "objMatrix");
        // 获取程序中顶点位置属性引用
        maPositionPointer = GLES30.glGetAttribLocation(mProgram, "objectPosition");

    }

    /**创造一个矩形面，并根据摄像机矩阵的变换更改自己的物体矩阵，使其始终面向视点**/
    private void initVertx() {
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertxData.length * 4);
        vbb.order(ByteOrder.nativeOrder());// 设置字节顺序
        mVertexBuffer = vbb.asFloatBuffer();// 转换为float型缓冲
        mVertexBuffer.put(vertxData);// 向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0);// 设置缓冲区起始位置
    }

    /**起点**/
    public void setStartVec(float xyz[]) {
        mStartVec[0] = xyz[0];
        mStartVec[1] = xyz[1];
        mStartVec[2] = xyz[2];
    }

    /**设置终点**/
    public void setEndVec(float xyz[]) {
        mEndVec[0] += xyz[0];
        mEndVec[1] += xyz[1];
        mEndVec[2] += xyz[2];
    }

    /**进行绘制**/
    public void draw() {
        GLES30.glUseProgram(mProgram);
//        MatrixState.pushMatrix();
//        //设置沿Z轴正向位移1
//        MatrixState.translate(0, 0, 1);
        MatrixState.pushMatrix();
        MatrixState.reverseTotalRotate();

        //todo 起点变换:
        MatrixState.translate(mStartVec[0], mStartVec[1], mStartVec[1]); //仅对光点的世界坐标变换矩阵临时修改，这样就可以实现不改顶点但只改光点的位置了。
//        //todo 终点变换(利用scale和rotate): 1、算一下终点向量对比起点向量的夹角，使用这个夹角旋转 2、计算终点向量的长度 - 起点长度的长度的向量，差使用scale乘以
        float arrowVec[] = new float[] {0f, 1f, 0};
        float xRotate = (float) calcAngleOfVectorsOnXYPanel(arrowVec, mEndVec);
        Log.i("cjztest", "xRotate:" + xRotate);
        MatrixState.rotate((float) calcAngleOfVectorsOnXYPanel(arrowVec, mEndVec), 0, 0, 1); //沿Z轴的旋转量
//        MatrixState.rotate((float) calcAngleOfVectorsOnXZPanel(arrowVec, mEndVec), 0, 1, 0); //沿Y轴的旋转量
//        MatrixState.rotate((float) calcAngleOfVectorsOnYZPanel(arrowVec, mEndVec), 1, 0, 0); //沿Y轴的旋转量

        MatrixState.scale((float) Math.sqrt(Math.pow(mEndVec[0], 2) + Math.pow(mEndVec[1], 2)));

        GLES30.glUniformMatrix4fv(mObjMatrixHandle, 1, false,
                MatrixState.getMMatrix(), 0);




        // 将最终变换矩阵传入渲染管线
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        MatrixState.popMatrix();
        //绘制光点
        GLES30.glVertexAttribPointer(maPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
        GLES30.glEnableVertexAttribArray(maPositionPointer); //启用顶点属性
        GLES30.glLineWidth(8f);
        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, vertxData.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        GLES30.glDisableVertexAttribArray(maPositionPointer); //启用顶点属性
//        MatrixState.popMatrix();
    }

    //todo XY平面上的的旋转量
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

    private double calcAngleOfVectorsOnXZPanel(float vec0[], float vec1[]) {
        double distanceOfVec0 = Math.sqrt(Math.pow(vec0[0], 2) + Math.pow(vec0[2], 2));
        double distanceOfVec1 = Math.sqrt(Math.pow(vec1[0], 2) + Math.pow(vec1[2], 2));
        double dotProduct = vec0[0] * vec1[0] + vec0[2] * vec1[2];
        double angle = Math.toDegrees(Math.acos(dotProduct / (distanceOfVec0 * distanceOfVec1)));
        if (vec1[0] > 0) {
            angle = 360 - angle;
        }
        return angle;
    }

    private double calcAngleOfVectorsOnYZPanel(float vec0[], float vec1[]) {
        double distanceOfVec0 = Math.sqrt(Math.pow(vec0[1], 2) + Math.pow(vec0[2], 2));
        double distanceOfVec1 = Math.sqrt(Math.pow(vec1[1], 2) + Math.pow(vec1[2], 2));
        double dotProduct = vec0[1] * vec1[1] + vec0[2] * vec1[2];
        double angle = Math.toDegrees(Math.acos(dotProduct / (distanceOfVec0 * distanceOfVec1)));
        if (vec1[0] > 0) {
            angle = 360 - angle;
        }
        return angle;
    }


}
