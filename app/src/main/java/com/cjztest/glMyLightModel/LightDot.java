package com.cjztest.glMyLightModel;

import android.content.res.Resources;
import android.opengl.GLES30;
import com.book2.Sample6_3.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**光斑**/
public class LightDot {
    private final Resources mRsc;
    private FloatBuffer mVertexBuffer;// 顶点坐标数据缓冲
    private FloatBuffer mUVBuffer;// 顶点坐标数据缓冲
    private String mVertexShader;// 顶点着色器代码脚本
    private String mFragmentShader;// 片元着色器代码脚本
    private int mProgram;
    private int muMVPMatrixHandle;
    private int mObjMatrixHandle;
    private int maPositionPointer;
    private int mVTexCoordPointer;
    private float vertxData[] = {//光斑的面，使用Z型顶点构造
            0.5f, -0.5f, 0,
            -0.5f, -0.5f, 0,
            0.5f, 0.5f, 0,
            -0.5f, 0.5f, 0
    };
    private float mTrans[] = new float[3];

    public LightDot(Resources resources, float screenRatio) {
        mRsc = resources;
        initShader();
        initVertx();
    }

    /**加载光点显示**/
    public void initShader() {
        // 加载顶点着色器的脚本内容
        mVertexShader = ShaderUtil.loadFromAssetsFile("cjztest/lightmodel/vertex.shader",
                mRsc);
        // 加载片元着色器的脚本内容
        mFragmentShader = ShaderUtil.loadFromAssetsFile("cjztest/lightmodel/fragShaderLightPot.shader",
                mRsc);
        // program索引
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //变换矩阵索引
        muMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        mObjMatrixHandle = GLES30.glGetUniformLocation(mProgram, "objMatrix");
        // 获取程序中顶点位置属性引用
        maPositionPointer = GLES30.glGetAttribLocation(mProgram, "objectPosition");
        //纹理采样坐标
        mVTexCoordPointer = GLES30.glGetAttribLocation(mProgram, "vTexCoord");

    }

    /**创造一个矩形面，并根据摄像机矩阵的变换更改自己的物体矩阵，使其始终面向视点**/
    private void initVertx() {
        float texCoor[] = new float[]   //纹理内采样坐标,类似于canvas坐标
                {
                        1.0f, 0.0f,
                        0.0f, 0.0f,
                        1.0f, 1.0f,
                        0.0f, 1.0f
                };
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertxData.length * 4);
        vbb.order(ByteOrder.nativeOrder());// 设置字节顺序
        mVertexBuffer = vbb.asFloatBuffer();// 转换为float型缓冲
        mVertexBuffer.put(vertxData);// 向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0);// 设置缓冲区起始位置

        ByteBuffer fragBB = ByteBuffer.allocateDirect(texCoor.length * 4);
        fragBB.order(ByteOrder.nativeOrder());// 设置字节顺序
        mUVBuffer = fragBB.asFloatBuffer();// 转换为float型缓冲
        mUVBuffer.put(texCoor);// 向缓冲区中放入顶点坐标数据
        mUVBuffer.position(0);// 设置缓冲区起始位置
    }

    /**设置光点的坐标——变换它的世界坐标**/
    public void setLocation(float xyz[]) {
        mTrans[0] = xyz[0];
        mTrans[1] = xyz[1];
        mTrans[2] = xyz[2];
    }

    /**移动光点**/
    public void translate(float xyz[]) {
        mTrans[0] += xyz[0];
        mTrans[1] += xyz[1];
        mTrans[2] += xyz[2];
    }

    public float[] getLightDotPos() {
        return mTrans;
    }

    /**进行绘制**/
    public void draw() {
        GLES30.glUseProgram(mProgram);
//        MatrixState.pushMatrix();
//        //设置沿Z轴正向位移1
//        MatrixState.translate(0, 0, 1);
        MatrixState.pushMatrix();
        MatrixState.reverseTotalRotate();

        MatrixState.translate(mTrans[0], mTrans[1], mTrans[1]); //仅对光点的世界坐标变换矩阵临时修改，这样就可以实现不改顶点但只改光点的位置了。
        GLES30.glUniformMatrix4fv(mObjMatrixHandle, 1, false,
                MatrixState.getMMatrix(), 0);
        // 将最终变换矩阵传入渲染管线
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        MatrixState.popMatrix();
        //绘制光点
        GLES30.glVertexAttribPointer(maPositionPointer, 3, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
        GLES30.glVertexAttribPointer(mVTexCoordPointer, 2, GLES30.GL_FLOAT, false, 0, mUVBuffer);  //二维向量，size为2
        GLES30.glEnableVertexAttribArray(maPositionPointer); //启用顶点属性
        GLES30.glEnableVertexAttribArray(mVTexCoordPointer); //启用顶点属性
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertxData.length / 3); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        GLES30.glDisableVertexAttribArray(maPositionPointer); //启用顶点属性
        GLES30.glDisableVertexAttribArray(mVTexCoordPointer); //启用顶点属性
//        MatrixState.popMatrix();
    }
}
