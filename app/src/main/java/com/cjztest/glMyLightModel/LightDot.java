package com.cjztest.glMyLightModel;

import android.content.res.Resources;
import android.opengl.GLES30;

import com.book2.Sample6_3.MatrixState;
import com.book2.Sample6_3.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**todo 由于纹理至少要一个面（因此至少要一个三角形）才能进行贴图，所以要弄出光斑效果，用两个三角形拼成光斑的外接矩形比较好**/
public class LightDot {
    private final Resources mRsc;
    private float mVertices[] = new float[3 ];
    private FloatBuffer mVertexBuffer;// 顶点坐标数据缓冲
    private String mVertexShader;// 顶点着色器代码脚本
    private String mFragmentShader;// 片元着色器代码脚本
    private int mProgram;
    private int muMVPMatrixHandle;
    private int maPositionHandle;

    public LightDot(Resources resources) {
        mRsc = resources;
        setLocation(new float[3]);
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
        // 获取程序中顶点位置属性引用
        maPositionHandle = GLES30.glGetAttribLocation(mProgram, "objectPosition");
    }

    /**todo 创造一个矩形面**/
    private void initVertx() {
        float texCoor[] = new float[]   //纹理内采样坐标,类似于canvas坐标
                {
                        0, 1,
                        0, 0,
                        1, 0,

                        1, 0,
                        1, 1,
                        0, 1,
                };
        ByteBuffer vbb = ByteBuffer.allocateDirect(mVertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());// 设置字节顺序
        mVertexBuffer = vbb.asFloatBuffer();// 转换为float型缓冲
        mVertexBuffer.put(mVertices);// 向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0);// 设置缓冲区起始位置
    }

    /**设置光点的坐标——变换它的世界坐标**/
    public void setLocation(float xyz[]) {

    }

    /**进行绘制**/
    public void draw() {
        GLES30.glUseProgram(mProgram);
        // 将最终变换矩阵传入渲染管线
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false,
                MatrixState.getFinalMatrix(), 0);
        //todo cjzmark 绘制光点
        GLES30.glLineWidth(15f);
        GLES30.glVertexAttribPointer(maPositionHandle, 3, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
        GLES30.glEnableVertexAttribArray(maPositionHandle); //启用顶点属性
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1); //绘制线条，添加的point浮点数/3才是坐标数（因为一个坐标由x,y,z3个float构成，不能直接用）
        GLES30.glDisableVertexAttribArray(maPositionHandle); //启用顶点属性
    }
}
